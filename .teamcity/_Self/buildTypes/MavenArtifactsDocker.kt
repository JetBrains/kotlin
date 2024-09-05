package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object MavenArtifactsDocker : BuildType({
    name = "🐧 Maven Artifacts Docker (no cache, native override)"
    description = "Run build for Maven artefacts with the scripts/build-kotlin-maven.sh in Docker container"

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
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("kotlin_deploy_version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin_native_version", "%kotlin_deploy_version%")
        param("reproducible.maven.artifact", "reproducible-maven-%kotlin_deploy_version%.zip")
        param("kotlin_docker_container", "kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v8")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Create build directory in advance so it has a build agent user"
            workingDir = "kotlin"
            scriptContent = "mkdir build"
        }
        script {
            name = "Build Maven Reproducible"
            scriptContent = """docker run --rm --workdir=/repo --volume=%teamcity.build.checkoutDir%/kotlin:/repo %kotlin_docker_container% /bin/bash -c "./scripts/build-kotlin-maven.sh %kotlin_deploy_version% '%kotlin_build_number%' %kotlin_native_version%""""
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
        exists("docker.version")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
        equals("teamcity.agent.hardware.cpuCount", "4")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
    }
})
