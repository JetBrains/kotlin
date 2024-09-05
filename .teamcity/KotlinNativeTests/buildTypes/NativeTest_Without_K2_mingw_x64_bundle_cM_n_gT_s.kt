package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object NativeTest_Without_K2_mingw_x64_bundle_cM_n_gT_s : BuildType({
    name = "🪟 Compiler Tests Without K2 (for bundle) opt.debug/cache.no/GC.STWMS (Native, Windows x86_64)"
    description = "Tests from the :native:native.tests project"

    artifactRules = """
        %kotlin.native.artifacts.logs%
        %kotlin.native.artifacts.llvm.dumps%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "%globalGradleBuildScanParameters% %globalGradleCacheNodeParameters% -Pkotlin.build.testRetry.maxRetries=0")
        param("kotlin.native.test_target", "mingw_x64")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%/test_dist")
        param("env.KONAN_USE_INTERNAL_SERVER", "1")
        param("kotlin.native.artifacts.llvm.dumps", "%system.teamcity.build.tempDir%/kotlin_native_llvm_module_dump*.ll")
        param("kotlin.native.artifacts.logs", "**/hs_err_pid*.log")
        text("requirement.jdk16", "%env.JDK_1_6%", display = ParameterDisplay.HIDDEN)
        param("konanVersion", "%kotlin.native.version%")
        text("requirement.jdk18", "%env.JDK_1_8%", display = ParameterDisplay.HIDDEN)
        text("requirement.jdk17", "%env.JDK_1_7%", display = ParameterDisplay.HIDDEN)
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
                -Pkotlin.internal.native.test.optimizationMode=DEBUG
                -Pkotlin.internal.native.test.cacheMode=NO
                -Pkotlin.internal.native.test.gcType=STWMS
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                "-Pkotlin.native.tests.tags=!frontend-fir"
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
                -Pkotlin.internal.native.test.optimizationMode=DEBUG
                -Pkotlin.internal.native.test.cacheMode=NO
                -Pkotlin.internal.native.test.gcType=STWMS
                -Pkotlin.internal.native.test.target=%kotlin.native.test_target%
                -Pbuild.number=%kotlin.native.version%
                "-Pkotlin.native.tests.tags=!frontend-fir"
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
        dependency(KotlinNative.buildTypes.KotlinNativeDist_mingw_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%.zip!kotlin-native-prebuilt-windows-x86_64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Windows")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        moreThan("teamcity.agent.hardware.memorySizeMb", "8100")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("teamcity.agent.jvm.os.name", "Windows")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        moreThan("teamcity.agent.hardware.memorySizeMb", "8100")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
