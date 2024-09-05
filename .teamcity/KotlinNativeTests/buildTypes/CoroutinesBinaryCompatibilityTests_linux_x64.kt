package KotlinNativeTests.buildTypes

import _Self.buildTypes.BuildNumber
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object CoroutinesBinaryCompatibilityTests_linux_x64 : BuildType({
    name = "🐧 Coroutines binary compatibility test (Native, Linux x86_64)"
    description = "Tests binary compatibility with Coroutines (Linux x86_64)"

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
        param("gradleParameters", "%globalGradleBuildScanParameters% %globalGradleCacheNodeParameters% -Pkotlin.build.testRetry.maxRetries=0")
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
        text("requirement.jdk11", "%env.JDK_11_0%", display = ParameterDisplay.HIDDEN)
    }

    vcs {
        root(DslContext.settingsRoot, "+:. => kotlin")
        root(KotlinNativeTests.vcsRoots.Kotlinx_Coroutines, "+:. => kotlinx.coroutines")

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
            name = "Compile and run Coroutines test (old compiler)"
            tasks = ":kotlinx-coroutines-core:compileTestKotlinLinuxX64 :kotlinx-coroutines-core:linkDebugTestLinuxX64 :kotlinx-coroutines-core:linuxX64Test"
            buildFile = "build.gradle"
            workingDir = "kotlinx.coroutines"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        script {
            workingDir = "kotlinx.coroutines"
            scriptContent = """
                cat > runOnlyTheseTasks.init.gradle.kts <<EOF
                               val allowedTaskPaths = gradle.startParameter.taskNames.toSet()
                
                gradle.addListener(object : BuildAdapter(), TaskExecutionListener {
                    val executedTaskPaths = mutableSetOf<String>()
                
                    override fun beforeExecute(task: Task) {}
                
                    @Synchronized
                    override fun afterExecute(task: Task, state: TaskState) {
                        if (!state.skipped)
                            executedTaskPaths += task.path
                    }
                
                    @Synchronized
                    override fun buildFinished(result: BuildResult) {
                        try {
                            // Should we also check order?
                            check(allowedTaskPaths == executedTaskPaths) {
                                buildString {
                                    appendln("Executed task mismatch:")
                                    append(" Expected: ")
                                    allowedTaskPaths.joinTo(this)
                                    appendln()
                                    append(" Actual: ")
                                    executedTaskPaths.joinTo(this)
                                    appendln()
                                    append("Make sure you use full task paths when invoking Gradle")
                                }
                            }
                        } finally {
                            executedTaskPaths.clear()
                        }
                    }
                })
                
                allprojects {
                    afterEvaluate {
                        tasks.configureEach {
                            if (this.path !in allowedTaskPaths) {
                                logger.info("DISABLE " + this)
                                this.enabled = false
                            }
                        }
                    }
                }
            """.trimIndent()
        }
        gradle {
            name = "Compile Coroutines test (old compiler)"
            tasks = "clean :kotlinx-coroutines-core:compileTestKotlinLinuxX64"
            buildFile = "build.gradle"
            workingDir = "kotlinx.coroutines"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Build and run Coroutines test (new compiler)"
            tasks = ":kotlinx-coroutines-core:linkDebugTestLinuxX64 :kotlinx-coroutines-core:linuxX64Test"
            buildFile = "build.gradle"
            workingDir = "kotlinx.coroutines"
            gradleParams = "%gradleParameters% --parallel -I runOnlyTheseTasks.init.gradle.kts -Pkotlin.native.home=%kotlin.native.test_dist%"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Compile Coroutines (old compiler)"
            tasks = "clean :kotlinx-coroutines-core:compileKotlinLinuxX64"
            buildFile = "build.gradle"
            workingDir = "kotlinx.coroutines"
            gradleParams = "%gradleParameters% --parallel"
            enableStacktrace = false
            jdkHome = "%env.JDK_11_0%"
        }
        gradle {
            name = "Build and run Coroutines test (new compiler) with Coroutines (built with old compiler)"
            tasks = ":kotlinx-coroutines-core:compileTestKotlinLinuxX64 :kotlinx-coroutines-core:linkDebugTestLinuxX64 :kotlinx-coroutines-core:linuxX64Test"
            buildFile = "build.gradle"
            workingDir = "kotlinx.coroutines"
            gradleParams = "%gradleParameters% --parallel -I runOnlyTheseTasks.init.gradle.kts -Pkotlin.native.home=%kotlin.native.test_dist%"
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
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        startsWith("teamcity.agent.jvm.os.name", "Linux")
        moreThan("teamcity.agent.work.dir.freeSpaceMb", "2048")
        equals("teamcity.agent.hardware.cpuCount", "8")
        noLessThan("teamcity.agent.hardware.memorySizeMb", "15000")
        noMoreThan("teamcity.agent.hardware.memorySizeMb", "17000")
        contains("system.cloud.profile_id", "-aws")
        startsWith("system.cloud.profile_id", "aquarius")
    }
})
