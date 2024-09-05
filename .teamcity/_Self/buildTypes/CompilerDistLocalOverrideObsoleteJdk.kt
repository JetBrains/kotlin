package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.Swabra
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.swabra
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object CompilerDistLocalOverrideObsoleteJdk : BuildType({
    name = "🐧 Compiler Dist (local build cache with JDK override)"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
    """.trimIndent()
    buildNumberPattern = "%build.number.default%"

    params {
        param("docker.image", "kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v8")
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("gradleParameters", "%globalGradleParameters% -Pkotlin.build.jar.compression=false -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
        param("DeployVersion", "default.snapshot")
        param("system.deployVersion", "%DeployVersion%")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        gradle {
            name = "Compiler & core libraries dist"
            tasks = "dist"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = "%gradleParameters% --parallel -Dscan.uploadInBackground=false -g %env.GRADLE_USER_HOME%"
            enableStacktrace = false
            dockerImage = "%docker.image%"
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3391"
            }
        }
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
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        exists("docker.version")
        equals("env.IS_GRADLE_USER_HOME_IN_TC_SYSTEM_DIR", "true")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
