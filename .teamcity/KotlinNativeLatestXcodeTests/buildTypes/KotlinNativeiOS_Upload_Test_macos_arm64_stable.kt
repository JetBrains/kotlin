package KotlinNativeLatestXcodeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativeiOS_Upload_Test_macos_arm64_stable : BuildType({
    name = "🍏ᵐ [Xcode Stable] iOS Upload Test (Native, Macos aarch64)"

    artifactRules = """
        **/hs_err*.log=>internal/hs_err.zip
        **/*.hprof=>internal/hprof.zip
        **/build/reports/dependency-verification=>internal/dependency-verification
        build/reports/configuration-cache/**/**/configuration-cache-report.html
        **/build/reports/tests/**=>internal/test_results.zip
        **/build/test-results/**=>internal/test_results.zip
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("jetbrains.sign.service.user", "%kotlin.native.ios.jetbrains.sign.service.user%")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
        param("system.spaceUsername", "%space.packages.user%")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        password("appstoreconnect.apiKey", "credentialsJSON:120fee1c-a7ba-4534-b9b5-f0f48f0cc99e")
        param("env.JDK_1_6", "%env.JDK_1_8%")
        password("system.spacePassword", "credentialsJSON:6d2dd689-93a7-4d0b-8175-4f470c0639c0")
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("env.JDK_1_7", "%env.JDK_1_8%")
        param("kotlin.native.target_opts", "")
        password("jetbrains.sign.service.secret", "credentialsJSON:22c52162-030a-463b-b6e9-d21e964f5c73")
        param("gradleParameters", "%globalGradleBuildScanParameters% -Pkotlin.build.testRetry.maxRetries=0 -Pkotlin.build.isObsoleteJdkOverrideEnabled=true -PcheckXcodeVersion=false")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        password("appstoreconnect.apiIssuer", "credentialsJSON:2c0449f4-5e6d-4410-809e-c190bfae3c23")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("teamcity.internal.gradle.runner.launch.mode", "gradle")
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        password("appstoreconnect.apiKeyId", "credentialsJSON:abb45139-06f9-48e0-a115-d6939271603b")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        text("requirement.jdk9", "%env.JDK_9_0%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk11", "%env.JDK_11_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(KotlinNative.vcsRoots.Kotlin_Native_IOS_Upload_Test, "+:. => kotlin-native-ios-upload-test")

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
            name = "iOS Upload Test"
            tasks = ":teamcity:run"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin-native-ios-upload-test"
            gradleParams = "%gradleParameters% --parallel -Pkonan.home=%kotlin.native.test_dist%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
    }

    failureConditions {
        executionTimeoutMin = 240
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(KotlinNativeDist_macos_arm64_BUNDLE_stable) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%.tar.gz!kotlin-native-prebuilt-macos-aarch64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        startsWith("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-stable-macos")
        noLessThanVer("tools.xcode.version", "13.0")
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        noLessThanVer("tools.xcode.version", "15.0")
    }
})
