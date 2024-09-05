package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object MavenArtifactsAgent : BuildType({
    name = "🐧 Maven Artifacts Agent (no cache, native override)"
    description = "Run build for Maven artefacts with the scripts/build-kotlin-maven.sh"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        kotlin/build/repo-reproducible/%reproducible.maven.artifact%
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("kotlin_build_number", "%build.number%")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("kotlin_deploy_version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin_native_version", "%kotlin_deploy_version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("reproducible.maven.artifact", "reproducible-maven-%kotlin_deploy_version%.zip")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Build Maven Reproducible"
            workingDir = "kotlin"
            scriptContent = """
                export PATH=${'$'}PATH:"%teamcity.tool.maven3_6%/bin"
                export JAVA_HOME=%env.JDK_11_0%
                ./scripts/build-kotlin-maven.sh %kotlin_deploy_version% '%kotlin_build_number%' %kotlin_native_version%
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 120
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
