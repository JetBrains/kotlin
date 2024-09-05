package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object PublishMavenArtifactsToGitHub : BuildType({
    name = "🐧 GitHub Release Page: publish maven sha256.txt artifacts"
    description = "Uploads maven-sha256.txt file to GitHub release page draft."

    artifactRules = "maven-${DeployKotlinMavenArtifacts.depParamRefs["DeployVersion"]}-sha256.txt"
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${DeployKotlinMavenArtifacts.depParamRefs.buildNumber}"

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        script {
            name = "Rename maven file and delete output file"
            scriptContent = """
                mv ${DeployKotlinMavenArtifacts.depParamRefs["reproducible.maven.artifact"]} maven-${DeployKotlinMavenArtifacts.depParamRefs["DeployVersion"]}.zip
                rm -f maven-${DeployKotlinMavenArtifacts.depParamRefs["DeployVersion"]}-sha256.txt
            """.trimIndent()
        }
        kotlinScript {
            name = "Generate a text file with hashes"
            content = """
                import java.io.*
                import java.security.DigestInputStream
                import java.security.MessageDigest
                import java.util.zip.ZipEntry
                import java.util.zip.ZipFile
                import kotlin.system.exitProcess
                
                fun exitWithError(message: String): Nothing {
                    System.err.println(message)
                    exitProcess(1)
                }
                
                fun main(args: Array<String>) {
                    val zipStr: String = args.getOrNull(0) ?: exitWithError("First parameter should be a path to zip file")
                    val zipFile = File(zipStr)
                    if (!zipFile.exists() || !zipFile.isFile || zipFile.extension != "zip") exitWithError("Path '${'$'}zipStr' is not a zip file")
                
                    val outputStr: String? = args.getOrNull(1)
                    if (outputStr != null) {
                        val outputFile = File(outputStr)
                        if (outputFile.exists()) exitWithError("File ${'$'}outputStr already exist")
                        FileOutputStream(outputFile).use { fos ->
                            PrintStream(fos).use { fps ->
                                count256Hashes(zipFile, fps)
                            }
                        }
                    } else {
                        count256Hashes(zipFile, System.out)
                    }
                }
                
                fun count256Hashes(zipFile: File, writer: PrintStream) {
                    val skipExtensions = setOf("sha1", "md5", "asc", "DS_Store")
                    writer.println("${'$'}{zipFile.name} ${'$'}{zipFile.sha256()}")
                
                    val zipZipFile = ZipFile(zipFile)
                    zipZipFile.entries()
                        .toList()
                        .filter { !it.isDirectory }
                        .filter { it.name.substringAfterLast(".") !in skipExtensions }
                        .sortedBy { it.name }
                        .forEach { entry ->
                            writer.println("${'$'}{entry.name} ${'$'}{entry.sha256(zipZipFile)}")
                        }
                }
                
                fun File.sha256() = inputStream().use { it.checksum("SHA-256") }
                fun ZipEntry.sha256(zipFile: ZipFile) = zipFile.getInputStream(this).use { it.checksum("SHA-256") }
                
                fun InputStream.checksum(digestName: String, bufferSize: Int = 2048): String {
                    val digest = MessageDigest.getInstance(digestName)
                
                    this.buffered(bufferSize).use { buffered ->
                        DigestInputStream(buffered, digest).use { dis ->
                            @Suppress("ControlFlowWithEmptyBody")
                            while (dis.read() != -1) {} // read to the end
                        }
                    }
                
                    return digest.digest().fold(StringBuilder()) { sb, it -> sb.append("%%02x".format(it)) }.toString()
                }
                
                main(args)
            """.trimIndent()
            arguments = "maven-${DeployKotlinMavenArtifacts.depParamRefs["DeployVersion"]}.zip maven-${DeployKotlinMavenArtifacts.depParamRefs["DeployVersion"]}-sha256.txt"
            jdkHome = "%env.JDK_11_0%"
        }
        kotlinScript {
            name = "Deploy maven artifacts to GitHub"
            workingDir = "kotlin"
            content = """
                import java.io.File
                import java.io.InputStream
                import java.net.URL
                import java.util.logging.Logger
                import javax.net.ssl.HttpsURLConnection
                
                val logger: Logger = Logger.getLogger("Kotlin settings logger")
                
                val repoOwner = "JetBrains"
                val repoName = "kotlin"
                
                
                fun main(args: Array<String>) {
                    val releaseId: String =
                        args.getOrNull(0) ?: error("Release id must be specified in as the 1st argument")
                    val token =
                        args.getOrNull(1) ?: error("Auth token must be specified in as the 2th argument")
                    val assetsPath =
                        args.getOrNull(2) ?: error("Assets path must be specified in as the 3rd argument")
                    val assets =
                        args.getOrNull(3) ?: error("Assets spitted by a comma must be specified in as the 4th argument")
                
                    uploadAssets(releaseId, parseFilenames(assets), assetsPath, token)
                }
                
                fun parseFilenames(argument: String): Set<String> {
                    logger.info("Passed in arguments files to upload: ${'$'}argument")
                    val filenames = mutableSetOf<String>()
                
                    val files = argument.split(",".toRegex()).map { it.trim() }
                    for (file in files) {
                        if (file.isNotEmpty()) {
                            filenames.add(file)
                        }
                    }
                    logger.info("Defined files to upload: ${'$'}filenames")
                    return filenames
                }
                
                fun uploadAssets(releaseId: String, assetFilenames: Set<String>, assetsPath: String, token: String) {
                    logger.info("Uploading release assets to the release page with ID ${'$'}releaseId...")
                
                    val errors: MutableList<String> = mutableListOf()
                
                    assetFilenames.forEach { filename ->
                        val assetFile = File("${'$'}assetsPath/${'$'}filename")
                        if (assetFile.isFile) {
                            logger.info("Uploading ${'$'}{assetFile.name} from ${'$'}{assetFile.path}")
                            val uploadAssetResponse = postRequest(
                                url = "https://uploads.github.com/repos/${'$'}repoOwner/${'$'}repoName/releases/${'$'}releaseId/assets?name=${'$'}{assetFile.name}",
                                requestProperties = mapOf(
                                    "Accept" to "application/vnd.github+json",
                                    "Authorization" to "Bearer ${'$'}token",
                                    "X-GitHub-Api-Version" to "2022-11-28",
                                    "Content-Type" to "application/octet-stream",
                                    "Content-Disposition" to "attachment"
                                ),
                                file = assetFile
                            )
                
                            val responseCode = uploadAssetResponse?.responseCode
                
                            if (responseCode == HttpsURLConnection.HTTP_CREATED) {
                                logger.info("${'$'}filename Uploaded successfully")
                            } else {
                                val logMessage = "Failed to upload ${'$'}filename. Response code: ${'$'}responseCode"
                                errors.add(logMessage)
                                logger.info(logMessage)
                            }
                        } else {
                            val logMessage = "Failed to parse a file ${'$'}filename"
                            errors.add(logMessage)
                            logger.info(logMessage)
                        }
                    }
                    if (errors.isNotEmpty()) {
                        error(
                            ${TQ}Failure while uploading release assets:
                                    |${'$'}{errors.joinToString("\n|")}
                                    |Please, make sure all necessary files have been uploaded${TQ}.trimMargin()
                        )
                    }
                }
                
                fun postRequest(
                    url: String, requestProperties: Map<String, String>, file: File? = null
                ): HttpRequestData? {
                    val connection = URL(url).openConnection() as HttpsURLConnection
                    var httpRequestData: HttpRequestData? = null
                    var inputStream: InputStream? = null
                    try {
                        connection.apply {
                            requestMethod = "POST"
                            doOutput = true
                            requestProperties.forEach {
                                setRequestProperty(it.key, it.value)
                            }
                            file?.inputStream()?.use { fileInputStream ->
                                fileInputStream.copyTo(outputStream)
                            }
                        }
                
                        val responseCode = connection.responseCode
                        inputStream = connection.inputStream
                        httpRequestData = HttpRequestData(responseCode)
                    } catch (e: Exception) {
                        logger.info("Error while sending request: ${'$'}{e.message}")
                    } finally {
                        logger.info("Disconnecting...")
                        connection.disconnect()
                        inputStream?.close()
                    }
                    return httpRequestData
                }
                
                
                data class HttpRequestData(
                    val responseCode: Int,
                )
                
                main(args)
            """.trimIndent()
            arguments = """
                ${DeployReleasePageDraftToGitHub.depParamRefs["github.releasePage.id"]}
                %github.KotlinBuild.auth.token% 
                %teamcity.build.checkoutDir%
                "maven-${BuildNumber.depParamRefs["deployVersion"]}-sha256.txt"
            """.trimIndent()
        }
    }

    features {
        freeDiskSpace {
            requiredSpace = "10gb"
            failBuild = true
        }
        perfmon {
        }
    }

    dependencies {
        dependency(DeployKotlinMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "${DeployKotlinMavenArtifacts.depParamRefs["reproducible.maven.artifact"]}"
            }
        }
        snapshot(DeployReleasePageDraftToGitHub) {
        }
        snapshot(Tests_Linux.buildTypes.TestReproducibleDeployMavenVsDocker) {
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("system.cloud.profile_id", "-deployment")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
