package Deploy.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object DeployReleasePageDraftToGitHub : BuildType({
    name = "🐧 GitHub Release Page: Release page draft to GitHub"
    description = "Creates a release page draft"

    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}"

    params {
        text("github.releasePage.id", "undefined", description = "this parameter should be set dynamically as a result of step 1 execution", display = ParameterDisplay.PROMPT)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.MANUAL
    }

    steps {
        kotlinScript {
            name = "Deploy Release page draft to GitHub"
            workingDir = "kotlin"
            content = """
                import java.io.InputStream
                import java.net.URL
                import java.util.logging.Logger
                import javax.net.ssl.HttpsURLConnection
                
                val logger: Logger = Logger.getLogger("Kotlin settings logger")
                
                val repoOwner = "JetBrains"
                val repoName = "kotlin"
                
                
                fun main(args: Array<String>) {
                    val releaseBranch: String =
                        args.getOrNull(0) ?: error("Release Branch must be specified in as the 1st argument")
                    val isEap =
                        args.getOrNull(1) ?: error("PreRelease flag must be specified in as the 2nd argument")
                    val deployVersion =
                        args.getOrNull(2) ?: error("Deploy version must be specified in as the 3d argument")
                    val token =
                        args.getOrNull(3) ?: error("Auth token must be specified in as the 4th argument")
                
                    val tagName = "v${'$'}deployVersion"
                
                
                    // Step: Create a draft release page
                    val createReleasePageDraftResponse: HttpRequestData = postRequest(
                        url = "https://api.github.com/repos/${'$'}repoOwner/${'$'}repoName/releases", requestProperties = mapOf(
                            "Accept" to "application/vnd.github+json",
                            "Authorization" to "Bearer ${'$'}token",
                            "X-GitHub-Api-Version" to "2022-11-28"
                        ), body = $TQ
                        {
                            "tag_name": "${'$'}tagName",
                            "name": "Kotlin ${'$'}deployVersion",
                            "draft": true,
                            "target_commitish": "${'$'}releaseBranch",
                            "prerelease": ${'$'}isEap
                        }
                    ${TQ}.trimIndent()
                    ) ?: error(
                        ${TQ}Error creating release JSON with tag ${'$'}tagName${TQ}.trimMargin()
                    )
                
                    if (createReleasePageDraftResponse.responseCode == HttpsURLConnection.HTTP_OK ||
                        createReleasePageDraftResponse.responseCode == HttpsURLConnection.HTTP_CREATED
                    ) {
                
                        val releasePageID = createReleasePageDraftResponse.getId()
                        logger.info("release page draft has been created with ID ${'$'}releasePageID")
                        println("##teamcity[setParameter name='github.releasePage.id' value='${'$'}{releasePageID}']")
                    } else {
                        error(
                            ${TQ}Error creating release page draft with tag ${'$'}tagName. Response code: ${'$'}{createReleasePageDraftResponse.responseCode}$TQ
                        )
                    }
                
                
                }
                
                fun postRequest(
                    url: String, requestProperties: Map<String, String>, body: String = ""
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
                            if (body.isNotEmpty()) {
                                outputStream.write(body.toByteArray())
                                outputStream.close()
                            }
                        }
                
                        val responseCode = connection.responseCode
                        inputStream = connection.inputStream
                        val responseText = inputStream.bufferedReader().use { it.readText() }
                        httpRequestData = HttpRequestData(responseCode, responseText)
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
                    val json: String
                ) {
                    fun getId() = json.substringAfter("\"id\":").substringBefore(",").toLong()
                }
                
                main(args)
            """.trimIndent()
            arguments = """
                %teamcity.build.branch% 
                true
                ${BuildNumber.depParamRefs["deployVersion"]} 
                %github.KotlinBuild.auth.token%
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
