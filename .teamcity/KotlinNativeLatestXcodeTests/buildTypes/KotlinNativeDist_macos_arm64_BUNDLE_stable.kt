package KotlinNativeLatestXcodeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.freeDiskSpace
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.retryBuild

object KotlinNativeDist_macos_arm64_BUNDLE_stable : BuildType({
    name = "🍏ᵐ [Xcode Stable] Compiler Dist: full bundle (Native, Macos aarch64)"
    description = "Build configuration"

    artifactRules = """
        kotlin/build/repo => native-maven
        kotlin/kotlin-native/kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz
        kotlin/kotlin-native/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz
        kotlin/kotlin-native/build/kotlin-native-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
        kotlin/kotlin-native/build/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz.sha256
        kotlin/kotlin-native/build/spdx/regular/kotlin-native-macos-aarch64-%kotlin.native.version%.spdx.json
        kotlin/kotlin-native/build/spdx/prebuilt/kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.spdx.json
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "--info %globalGradleBuildScanParameters% -Pkotlin.build.isObsoleteJdkOverrideEnabled=true -Pkotlin.native.platformLibs.updateDefFileDependencies=%kotlin.native.platformLibs.updateDefFileDependencies%")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        param("kotlin.native.platformLibs.updateDefFileDependencies", "false")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_1_6", "%env.JDK_1_8%")
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        param("system.deployVersion", "%kotlin.native.version%")
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("env.JDK_1_7", "%env.JDK_1_8%")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    steps {
        script {
            name = "Set up Git"
            scriptContent = """
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" config user.email teamcity-demo-noreply@jetbrains.com
                "%env.TEAMCITY_GIT_PATH%" -C "%teamcity.build.checkoutDir%/kotlin" config user.name TeamCity
            """.trimIndent()
        }
        script {
            name = "Print KONAN_USE_INTERNAL_SERVER value"
            scriptContent = "printenv | grep KONAN_USE_INTERNAL_SERVER || true"
        }
        script {
            name = "Print current Xcode version"
            scriptContent = "xcode-select -p"
        }
        script {
            name = "Print processes list sorted by memory usage"
            scriptContent = "echo '%MEM    RSS      VSZ   PID     ELAPSED USER             COMMAND'; ps -aexo pmem,rss,vsize,pid,etime,user,command | sort -r"
        }
        gradle {
            name = "Build Compiler Dist: full bundle"
            tasks = ":kotlin-native:publish"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel -Pbuild.number=%kotlin.native.version%
                -Pkotlin.native.enabled=true
                -Pkotlin.native.allowRunningCinteropInProcess=true
                -Pkotlin.incremental=false
            """.trimIndent()
            enableStacktrace = true
            jdkHome = "%env.JDK_17_0%"
        }
    }

    triggers {
        retryBuild {
            attempts = 2
            moveToTheQueueTop = true
            branchFilter = "+:master"
        }
    }

    failureConditions {
        executionTimeoutMin = 200
    }

    features {
        freeDiskSpace {
            requiredSpace = "32gb"
            failBuild = true
        }
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        startsWith("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-stable-macos")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
    }
})
