package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object DeployCompilerArtifactsToGitHub : BuildType({
    name = "🐧 GitHub Release Page: Deploy compiler artifacts to GitHub"
    description = "Deploys to the github release page kotlin compiler artifacts archives, including kotlin native and spdx.json files."

    artifactRules = """
        kotlin-compiler-*
        kotlin-native-*
    """.trimIndent()
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        kotlinScript {
            name = "Deploy compiler artifacts to GitHub"
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
                "kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.zip, kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.zip.sha256, kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.spdx.json, kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz, kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz.sha256, kotlin-native-prebuilt-linux-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.spdx.json, kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz, kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz.sha256, kotlin-native-prebuilt-macos-aarch64-${BuildNumber.depParamRefs["deployVersion"]}.spdx.json, kotlin-native-prebuilt-macos-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz, kotlin-native-prebuilt-macos-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.tar.gz.sha256, kotlin-native-prebuilt-macos-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.spdx.json, kotlin-native-prebuilt-windows-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.zip, kotlin-native-prebuilt-windows-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.zip.sha256, kotlin-native-prebuilt-windows-x86_64-${BuildNumber.depParamRefs["deployVersion"]}.spdx.json"
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
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(_Self.buildTypes.CompilerArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.zip
                    +:kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.zip.sha256
                    +:kotlin-compiler-${BuildNumber.depParamRefs["deployVersion"]}.spdx.json
                """.trimIndent()
            }
        }
        snapshot(DeployReleasePageDraftToGitHub) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-linux-x86_64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-linux-x86_64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        dependency(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE_beta) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        dependency(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE_custom) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        dependency(KotlinNativeLatestXcodeTests.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE_stable) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-macos-x86_64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-macos-x86_64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-macos-x86_64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz.sha256
                    +:kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz
                    +:kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_mingw_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = """
                    +:kotlin-native-windows-x86_64-%kotlin.native.version%.zip
                    +:kotlin-native-windows-x86_64-%kotlin.native.version%.zip.sha256
                    +:kotlin-native-windows-x86_64-%kotlin.native.version%.spdx.json
                    +:kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip.sha256
                    +:kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip
                    +:kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.spdx.json
                """.trimIndent()
            }
        }
        snapshot(Tests_Linux.buildTypes.TestReproducibleCompilerDockerVsAgent) {
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
