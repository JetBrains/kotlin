package _Self.buildTypes

import KotlinNative.buildTypes.KotlinNativeDist_linux_x64_LIGHT_BUNDLE
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object LibraryReferenceLegacyDocs : BuildType({
    name = "🐧 Libraries Reference Documentation (with legacy Dokka)"

    buildNumberPattern = "%build.number.default%"

    params {
        param("gradleParameters", "%globalGradleParameters%")
        param("konanVersion", "${KotlinNativeDist_linux_x64_LIGHT_BUNDLE.depParamRefs["konanVersion"]}")
        param("system.deployVersion", "${CompilerDistAndMavenArtifacts.depParamRefs["system.deployVersion"]}")
        param("system.githubRevision", "${DslContext.settingsRoot.paramRefs.buildVcsNumber}")
        param("system.kotlinLibsRepo", "%teamcity.build.checkoutDir%/kotlin/build/repo")
        param("build.number.default", "${BuildNumber.depParamRefs.buildNumber}")
        param("system.kotlinNativeDistDir", "%teamcity.build.checkoutDir%/kotlin/kotlin-native/dist")
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        gradle {
            name = "Docs"
            tasks = "callDokka"
            buildFile = "build.gradle"
            workingDir = "%teamcity.build.checkoutDir%/kotlin/libraries/tools/kotlin-stdlib-docs-legacy"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = true
            jdkHome = "%env.JDK_11_0%"
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
        snapshot(BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(CompilerDistAndMavenArtifacts) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "maven.zip!org/jetbrains/kotlin/**=>%system.kotlinLibsRepo%/org/jetbrains/kotlin/"
            }
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_LIGHT_BUNDLE) {
            snapshot {
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-linux-x86_64-%konanVersion%.tar.gz!kotlin-native-prebuilt-linux-x86_64-%konanVersion%/klib/**=>%system.kotlinNativeDistDir%/klib"
            }
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
