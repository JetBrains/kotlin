package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object NativeTest_K2_Without_PL_ios_arm64_bundle_oM_o_cM_n_sTE_e_eGC_e_x_e : BuildType({
    name = "🍏ᵐ Compiler Tests K2 Without PL (for bundle) opt.opt/cache.no/sharedExec/eagerGroup/xctest (Native, iOS ARM64)"
    description = "Tests from the :native:native.tests project"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("kotlin.native.test.gcloud.project", "kotlin-native-test-633ea")
        param("gradleParameters", "%globalGradleBuildScanParameters% -Pkotlin.build.testRetry.maxRetries=0 -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
        param("kotlin.native.test_target", "ios_arm64")
        param("env.CLOUDSDK_CORE_DISABLE_PROMPTS", "1")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        password("gcloud.keyfile", "credentialsJSON:6796477c-df36-43d6-90e0-6d31cbb9a67b")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("env.HOMEBREW_NO_INSTALL_UPGRADE", "1")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%/test_dist")
        param("env.HOMEBREW_NO_INSTALLED_DEPENDENTS_CHECK", "1")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        param("kotlin.native.test.gcloud.account", "buildserver-service@kotlin-native-test-633ea.iam.gserviceaccount.com")
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
        script {
            name = "Install Google Cloud SDK using Brew"
            scriptContent = "brew install --cask google-cloud-sdk"
        }
        script {
            name = "Write a key to a file"
            scriptContent = "echo %gcloud.keyfile% | base64 --decode > %teamcity.build.checkoutDir%/kotlin/account-key-file.json"
        }
        script {
            name = "Activate Firebase service account"
            scriptContent = "gcloud auth activate-service-account %kotlin.native.test.gcloud.account% --key-file=%teamcity.build.checkoutDir%/kotlin/account-key-file.json --project=%kotlin.native.test.gcloud.project%"
        }
        script {
            name = "Delete account key after the activation"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = "rm %teamcity.build.checkoutDir%/kotlin/account-key-file.json"
        }
        script {
            name = "List available iOS models in Firebase to test CLI"
            scriptContent = "gcloud firebase test ios models list"
        }
        gradle {
            name = "Compile everything before running tests"
            tasks = ":native:native.tests:test"
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
                -Pkotlin.internal.native.test.sharedTestExecution=true
                -Pkotlin.internal.native.test.eagerGroupCreation=true
                -Pkotlin.internal.native.test.xctest=true
                -Pbuild.number=%kotlin.native.version%
                "-Pkotlin.native.tests.tags=frontend-fir&no-partial-linkage-may-be-skipped"
                -Pkotlin.build.disable.verification.tasks=true -Dscan.tag.verification-tasks-disabled
            """.trimIndent()
            enableStacktrace = true
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Native compiler tests"
            tasks = ":native:native.tests:test"
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
                -Pkotlin.internal.native.test.sharedTestExecution=true
                -Pkotlin.internal.native.test.eagerGroupCreation=true
                -Pkotlin.internal.native.test.xctest=true
                -Pbuild.number=%kotlin.native.version%
                "-Pkotlin.native.tests.tags=frontend-fir&no-partial-linkage-may-be-skipped"
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_macos_arm64_BUNDLE) {
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
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-beta-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-latest-xcode-kmp-test-macos")
        exists("tools.xcode.platform.iphoneos")
        noLessThanVer("tools.xcode.version", "13.0")
        startsWith("teamcity.agent.jvm.os.name", "Mac")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        contains("teamcity.agent.jvm.os.arch", "aarch64")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-stable-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-native-xcode-beta-macos")
        doesNotContain("teamcity.agent.name", "aquarius-kotlin-k8s-latest-xcode-kmp-test-macos")
    }
})
