package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object NativeTest_K2_Only_PL_macos_x64_bundle_oM_o_cM_n : BuildType({
    name = "🍎 Compiler Tests K2 Only PL (for bundle) opt.opt/cache.no (Native, Macos x86_64)"
    description = "Tests from the :native:native.tests project"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "%globalGradleBuildScanParameters% -Pkotlin.build.testRetry.maxRetries=0 -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
        param("kotlin.native.test_target", "macos_x64")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%/test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_1_6", "%env.JDK_1_8%")
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_17_0%", display = ParameterDisplay.HIDDEN)
        param("env.JDK_9_0", "%env.JDK_11_0%")
        param("env.JDK_1_7", "%env.JDK_1_8%")
        param("konanMetaVersion", "%build.number.native.meta.version%")
        param("kotlin.native.target_opts", "")
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
            name = "Compile everything before running tests"
            tasks = ":nativeCompilerTest"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel --continue
                -Pkotlin.native.enabled=true
                -Pkotlin.internal.native.test.nativeHome=%kotlin.native.test_dist%
                -Pkotlin.incremental=false
                -Pkotlin.internal.native.test.mode=TWO_STAGE_MULTI_MODULE
                -Pkotlin.internal.native.test.optimizationMode=OPT
                -Pkotlin.internal.native.test.cacheMode=NO
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                "-Pkotlin.native.tests.tags=frontend-fir&!no-partial-linkage-may-be-skipped"
                -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled
            """.trimIndent()
            enableStacktrace = true
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Native compiler tests"
            tasks = ":nativeCompilerTest"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel --continue
                -Pkotlin.native.enabled=true
                -Pkotlin.internal.native.test.nativeHome=%kotlin.native.test_dist%
                -Pkotlin.incremental=false
                -Pkotlin.internal.native.test.mode=TWO_STAGE_MULTI_MODULE
                -Pkotlin.internal.native.test.optimizationMode=OPT
                -Pkotlin.internal.native.test.cacheMode=NO
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                "-Pkotlin.native.tests.tags=frontend-fir&!no-partial-linkage-may-be-skipped"
            """.trimIndent()
            enableStacktrace = true
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%.tar.gz!kotlin-native-prebuilt-macos-x86_64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        startsWith("teamcity.agent.name", "kotlin-macos-x64-")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "x86_64")
        noLessThanVer("tools.xcode.version", "15.0")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-beta-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-latest-xcode-kmp-test-macos")
        noLessThanVer("tools.xcode.version", "13.0")
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        startsWith("teamcity.agent.name", "kotlin-macos-x64-")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "x86_64")
        noLessThanVer("tools.xcode.version", "15.0")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-beta-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-latest-xcode-kmp-test-macos")
    }
})
