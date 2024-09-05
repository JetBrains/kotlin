package KotlinNativePerformanceTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object KotlinNativePerformance_Tests_New_MM_Pre_commit_linux_x64 : BuildType({
    name = "🐧 Performance Tests MM.exp with Generation of Platform Libs (Native, Linux x86_64)"

    artifactRules = """
        kotlin/kotlin-native/performance/build/nativeReport.json
        kotlin/kotlin-native/report/report.html => report
        kotlin/kotlin-native/targetsResult/Linux.txt
        %kotlin.native.artifacts.logs%
    """.trimIndent()
    buildNumberPattern = "%kotlin.native.version%"

    params {
        param("gradleParameters", "%globalGradleParameters% -Pkotlin.build.isObsoleteJdkOverrideEnabled=true")
        param("env.BUILD_TYPE", "DEV")
        password("system.artifactory.apikey", "credentialsJSON:edd14ef2-4f23-4527-b823-1d4407091e2c")
        param("kotlin.native.performance.server.url", "https://kotlin-native-perf-summary-internal.labs.jb.gg")
        param("kotlin.native.version", "${BuildNumber.depParamRefs["deployVersion"]}")
        param("kotlin.native.test_dist", "%teamcity.build.checkoutDir%%teamcity.agent.jvm.file.separator%test_dist")
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
            name = "platform libs"
            tasks = ":kotlin-native:platformLibs:linux_x64-posix"
            buildFile = "build.gradle.kts"
            workingDir = "%teamcity.build.checkoutDir%/kotlin"
            gradleParams = """
                %gradleParameters% --parallel -Pbuild.number=%kotlin.native.version%
                -PkonanVersion=%konanVersion% -Pkotlin.native.home=%kotlin.native.test_dist% -Pkotlin.native.enabled=true -i -Pkotlin.native.performance.server.url=%kotlin.native.performance.server.url% --no-configuration-cache
            """.trimIndent()
            enableStacktrace = false
        }
        script {
            name = "Set cset"
            enabled = false
            scriptContent = "sudo cset shield -c 0,1"
        }
        gradle {
            name = "Gradle performance"
            tasks = ":clean :buildAnalyzer :konanRun"
            buildFile = "build.gradle"
            workingDir = "kotlin/kotlin-native/performance"
            gradleParams = """
                %gradleParameters% --no-parallel -Pbuild.number=%kotlin.native.version%
                -PkonanVersion=%konanVersion% -Pkotlin.native.home=%kotlin.native.test_dist% -Pkotlin.native.enabled=true -i -Pkotlin.native.performance.server.url=%kotlin.native.performance.server.url% --no-configuration-cache -Pkotlin_root=../..
            """.trimIndent()
            gradleWrapperPath = "../.."
            enableStacktrace = false
        }
        gradle {
            name = "TC statistics"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            tasks = ":teamCityStat"
            buildFile = "build.gradle"
            workingDir = "kotlin/kotlin-native/performance"
            gradleParams = """
                %gradleParameters% --parallel -Pbuild.number=%kotlin.native.version%
                -PkonanVersion=%konanVersion% -Pkotlin.native.home=%kotlin.native.test_dist% -Pkotlin.native.enabled=true -i -Pkotlin.native.performance.server.url=%kotlin.native.performance.server.url% --no-configuration-cache
            """.trimIndent()
            gradleWrapperPath = "../.."
            enableStacktrace = false
        }
        gradle {
            name = "Register build"
            tasks = ":registerBuild"
            buildFile = "build.gradle"
            workingDir = "kotlin/kotlin-native/performance"
            gradleParams = """
                %gradleParameters% --parallel -Pbuild.number=%kotlin.native.version%
                -PkonanVersion=%konanVersion% -Pkotlin.native.home=%kotlin.native.test_dist% -Pkotlin.native.enabled=true -i -Pkotlin.native.performance.server.url=%kotlin.native.performance.server.url% --no-configuration-cache -PbuildNumberSuffix=NewMM
            """.trimIndent()
            gradleWrapperPath = "../.."
            enableStacktrace = false
        }
    }

    failureConditions {
        executionTimeoutMin = 240
    }

    dependencies {
        snapshot(_Self.buildTypes.BuildNumber) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
        dependency(KotlinNative.buildTypes.KotlinNativeDist_linux_x64_BUNDLE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                artifactRules = "kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%.tar.gz!kotlin-native-prebuilt-linux-x86_64-%kotlin.native.version%/** => %kotlin.native.test_dist%"
            }
        }
    }

    requirements {
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        contains("teamcity.agent.name", "kotlin-linux-x64-metal-munit787")
    }

    cleanup {
        baseRule {
            history(days = 30)
            artifacts(days = 30, artifactPatterns = """
                +:*.hprof* 
                +:**/*.hprof* 
                +:*shutdown*.snapshot 
                +:**/*shutdown*.snapshot
            """.trimIndent())
        }
    }
})
