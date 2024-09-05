package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object JPSBuildTest_LINUX : BuildType({
    name = "🐧 Test JPS build"

    artifactRules = """
        jps/.idea => jps/idea.zip
        jps/out => jps/out.zip
        jps/dist => jps/dist.zip
        jpsBuildPlugin/local/system/log => idea_logs.zip
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("env.remote_cache_url", "https://temporary-files-cache.labs.jb.gg/cache/jps/kotlin/")
        param("gradleParameters", "--info --full-stacktrace")
        param("env.build_vcs_branch_kotlin", "${DslContext.settingsRoot.paramRefs.buildVcsBranch}")
        param("bootstrap.kotlin.version", "-- value is set in checkBuild gradle task --")
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("env.build_vcs_number_kotlin", "${DslContext.settingsRoot.paramRefs.buildVcsNumber}")
        param("env.ide_plugins", "java,gradle,org.jetbrains.kotlin:%plugin.bootstrap.kotlin.version%")
        param("plugin.bootstrap.kotlin.version", "-- value is set in Set params for bootstrap step --")
        param("bootstrap.kotlin.url", "-- value is set in Set params for bootstrap step --")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("requirement.jdk9", "%env.JDK_9_0%")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => jps")
        root(Tests_Linux.vcsRoots.JpsBuildTool, "+:. => jpsBuildPlugin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        kotlinScript {
            name = "Enable jps build with portable caches"
            content = """
                #!/usr/bin/env kotlin
                
                import java.io.File
                
                val localPropertiesFile = File("jps/local.properties")
                localPropertiesFile.writeText($TQ
                    jpsBuild=true
                    intellijUltimateEnabled=false
                    kotlin.build.dependencies.iu.enabled=false
                ${TQ}.trimIndent())
                
                //TODO: remove
                val gradlePropertiesFile = File("jps/gradle.properties")
                gradlePropertiesFile.appendText("\norg.gradle.dependency.verification=off")
            """.trimIndent()
        }
        gradle {
            name = "Check build"
            tasks = "checkBuild"
            buildFile = "build.gradle.kts"
            workingDir = "jps"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        kotlinScript {
            name = "Set params for bootstrap"
            content = """
                #!/usr/bin/env kotlin
                
                import java.net.HttpURLConnection
                import java.net.URL
                
                val url = URL("https://plugins.jetbrains.com/plugins/list?channel=bootstrap&pluginId=6954")
                
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET
                
                    println("Get bootstrap from: ${'$'}url; Response Code: ${'$'}responseCode")
                    val text = inputStream.bufferedReader().use {
                        it.readText()
                    }
                
                    val versionPattern = "211-%bootstrap.kotlin.version%"
                    val version = versionPattern + text.substringAfter("<version>${'$'}versionPattern").substringBefore("</version>")
                    println("Version is: ${'$'}version")
                    
                    val downloadUrl = text.substringAfter(version).substringAfter("<download-url>").substringBefore("</download-url>")
                    println("Download url is: ${'$'}downloadUrl")
                
                    println("##teamcity[setParameter name='plugin.bootstrap.kotlin.version' value='${'$'}version']")
                    println("##teamcity[setParameter name='bootstrap.kotlin.url' value='${'$'}downloadUrl']")
                }
            """.trimIndent()
        }
        ant {
            name = "Download Bootstrap plugin"
            mode = antScript {
                content = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project name="download-bootstrap-plugin" default="download">
                    	<target name="download">
                    		<get src="%bootstrap.kotlin.url%" dest="kotlin-plugin-%plugin.bootstrap.kotlin.version%.zip" />
                    	</target>
                    </project>
                """.trimIndent()
            }
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
        }
        maven {
            name = "Publish Kotlin IDE plugin to local repository"
            goals = "install:install-file"
            pomLocation = ""
            runnerArgs = """
                -Dfile=kotlin-plugin-%plugin.bootstrap.kotlin.version%.zip
                 -DgroupId=com.jetbrains.plugins -DartifactId=org.jetbrains.kotlin -Dversion=%plugin.bootstrap.kotlin.version% -Dpackaging=zip
            """.trimIndent()
            mavenVersion = bundled_3_6()
            localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT
        }
        gradle {
            name = "Import Kotlin project and build with JPS"
            tasks = "runIde"
            buildFile = "build.gradle"
            workingDir = "jpsBuildPlugin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = """
                +:<default>
                +:rr/*
                +:rrn/*
                +:push/*
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 60
        supportTestRetry = true
    }

    features {
        swabra {
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
        }
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
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
