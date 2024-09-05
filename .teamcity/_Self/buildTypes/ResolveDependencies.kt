package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object ResolveDependencies : BuildType({
    name = "🐧 Resolve Dependencies"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("gradleParameters", "%globalGradleParameters%")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Clean-up verification-metadata"
            scriptContent = """
                #!/bin/bash
                sed -i -e '/<components>/,/<\/components>/d' %teamcity.build.checkoutDir%/kotlin/gradle/verification-metadata.xml
            """.trimIndent()
        }
        gradle {
            name = "Resolve Dependencies & update verification-metadata"
            tasks = "resolveDependencies"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel --write-verification-metadata md5,sha256 -Pkotlin.native.enabled=true --no-configuration-cache"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            scriptContent = """
                #!/bin/bash
                echo "##teamcity[testStarted name='verification-metadata.Check if verification-metadata is up-to-date']"
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" --no-pager diff --patch --exit-code gradle/verification-metadata.xml
                if [ ${'$'}? -eq 0 ]; then
                    echo "##teamcity[testFinished name='verification-metadata.Check if verification-metadata is up-to-date']"
                else
                    echo "##teamcity[testFailed name='verification-metadata.Check if verification-metadata is up-to-date' message='gradle/verification-metadata.xml is not up-to-date' details='Update gradle/verification-metadata.xml as described in https://github.com/JetBrains/kotlin#dependency-verification']"
                    echo "##teamcity[testFinished name='verification-metadata.Check if verification-metadata is up-to-date']"
                fi
            """.trimIndent()
        }
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.DISABLED
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
        snapshot(BuildNumber) {
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
