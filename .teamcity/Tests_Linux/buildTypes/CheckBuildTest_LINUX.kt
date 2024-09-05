package Tests_Linux.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CheckBuildTest_LINUX : BuildType({
    name = "🐧 Check Build Test"

    buildNumberPattern = "%build.number.default%"

    params {
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("gradleParameters", "%globalGradleParameters%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Check buildSrc"
            tasks = "checkBuild"
            buildFile = "%teamcity.build.checkoutDir%/kotlin/repo/gradle-build-conventions/buildsrc-compat/build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Check build"
            tasks = "checkBuild"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            name = "Check configuration cache in dry runs"
            scriptContent = """
                #!/bin/bash
                            
                            cd kotlin
                            
                            function runTest {
                              local testName="Configuration cache for |'${'$'}1|'"
                              echo "##teamcity[testStarted name='${'$'}testName']"
                            
                              local exitCode
                              local gradleCommand="./gradlew ${'$'}1 -Pkotlin.build.gradle.publish.javadocs=false --configuration-cache --dry-run --no-daemon -Pkotlin.build.proguard=false"
                              local storeEntryOutput
                              storeEntryOutput=${'$'}(${'$'}gradleCommand 2>&1)
                              exitCode=${'$'}?
                              echo "${'$'}storeEntryOutput"
                              ([ ${'$'}exitCode -eq 0 ] && echo "${'$'}storeEntryOutput" | grep "Configuration cache entry stored." > /dev/null) || (reportTestFailure "${'$'}testName" "Configuration cache is not stored" && return)
                              local reuseEntryOutput
                              reuseEntryOutput=${'$'}(${'$'}gradleCommand 2>&1)
                              exitCode=${'$'}?
                              echo "${'$'}reuseEntryOutput"
                              ([ ${'$'}exitCode -eq 0 ] && echo "${'$'}reuseEntryOutput" | grep "Configuration cache entry reused." > /dev/null) || (reportTestFailure "${'$'}testName" "Configuration cache is not reused" && return)
                            
                              finishTest "${'$'}testName"
                            }
                            
                            function reportTestFailure {
                              if [ ${'$'}? -ne 0 ]; then
                                echo "##teamcity[testFailed name='${'$'}1' message='${'$'}2']"
                                finishTest "${'$'}1"
                              fi
                            }
                            
                            function finishTest {
                              echo "##teamcity[testFinished name='${'$'}1']"
                            }
                     
                            runTest "clean"
                runTest "install"
                runTest "gradlePluginIntegrationTest"
                runTest "miscCompilerTest"
                runTest "jps-tests"
                runTest "scriptingJvmTest"
                runTest "generateTests"
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 30
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
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
