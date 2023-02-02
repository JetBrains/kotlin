import org.jetbrains.kotlin.build.benchmarks.dsl.*

fun historyFilesBenchmarks() = kotlinBenchmarks(additionalDefaultProperties = arrayOf("-Pkotlin.incremental.useClasspathSnapshot=false"))
fun abiSnapshotBenchmarks() = kotlinBenchmarks(prefix = "ABI_SNAPSHOT: ", arrayOf("-Pkotlin.incremental.classpath.snapshot.enabled=true"))
fun artifactTransformBenchmarks() = kotlinBenchmarks(prefix = "TRANSFORMATION: ", arrayOf("-Pkotlin.incremental.useClasspathSnapshot=true"), withLatestLtsJdk = true)

//move prefix to suite
fun kotlinBenchmarks(prefix: String = "", additionalDefaultProperties: Array<String> = emptyArray(), withLatestLtsJdk: Boolean = false) =
    suite {
        val coreUtilStrings = changeableFile("coreUtil/StringsKt")
        val coreUtilCoreLib = changeableFile("coreUtil/CoreLibKt")
        val compilerCommonBackendContext = changeableFile("compiler/CommonBackendContext")
        val kotlinGradlePluginConfigurationPhaseAware = changeableFile("kotlinGradlePlugin/ConfigurationPhaseAware")
        val buildSrc = changeableFile("buildSrc/BuildPropertiesExtKt")

        defaultTasks(Tasks.DIST, Tasks.COMPILER_TEST_CLASSES, Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
        defaultJdk = System.getenv("JDK_8")

        val noArgs = arrayOf<String>()

        val defaultArguments = arrayOf(
            "--info",
            "--no-build-cache",
            "--watch-fs",
            *additionalDefaultProperties
        )

        val parallelRerunBuild = arrayOf(
            *defaultArguments,
            "--parallel",
            "--rerun-tasks"
        )

        val nonParallelBuild = arrayOf(
            *defaultArguments,
            "--no-parallel",
            "--max-workers=1",
        )

        val nonParallelRerunBuild = arrayOf(
            *nonParallelBuild,
            "--rerun-tasks",
        )

        defaultArguments(*defaultArguments)

        fun registerScenarios(withLatestLtsJdk: Boolean = false) {
            val jdkPrefix = if (withLatestLtsJdk) {
                "JDK 17 "
            } else {
                ""
            }
            val jdkPath = if (withLatestLtsJdk) {
                System.getenv("JDK_17_0")
            } else {
                defaultJdk
            }

            scenario("$jdkPrefix${prefix}parallel clean compile to warmup daemon") {
                jdk = jdkPath
                arguments(*parallelRerunBuild, "-x", "compileKotlinWasm")
                expectSlowBuild("clean build")
                step {
                    doNotMeasure()
                    runTasks("assemble")
                }
            }

            scenario("$jdkPrefix${prefix}(buildSrc, Kotlin) add public fun") {
                jdk = jdkPath
                step {
                    changeFile(buildSrc, TypeOfChange.ADD_PUBLIC_FUNCTION)
                    runTasks(*noArgs)
                }
            }

            scenario("$jdkPrefix${prefix}(buildSrc, Kotlin) add private fun") {
                jdk = jdkPath
                step {
                    changeFile(buildSrc, TypeOfChange.ADD_PRIVATE_FUNCTION)
                    runTasks(*noArgs)
                }
            }

            scenario("$jdkPrefix${prefix}clean build") {
                jdk = jdkPath
                arguments(*nonParallelRerunBuild, "-x", "compileKotlinWasm")
                expectSlowBuild("clean build")
                step {
                    doNotMeasure()
                    runTasks(Tasks.CLEAN)
                }
                step {
                    runTasks("assemble")
                }
            }

            scenario("$jdkPrefix${prefix}clean build parallel") {
                jdk = jdkPath
                arguments(*parallelRerunBuild, "-x", "compileKotlinWasm")
                expectSlowBuild("clean build")
                step {
                    doNotMeasure()
                    runTasks(Tasks.CLEAN)
                }
                step {
                    runTasks("assemble")
                }
            }

            scenario("$jdkPrefix${prefix}Run gradle plugin tests") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_TEST)
                }
                step {
                    doNotMeasure()
                    runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_TEST_CLEAN)
                }
                repeat = 5U
            }

            scenario("$jdkPrefix${prefix}Run gradle plugin tests after changes") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PRIVATE_FUNCTION)
                    runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_TEST)
                }
                step {
                    doNotMeasure()
                    runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_TEST_CLEAN)
                }
                repeat = 5U
            }

            scenario("$jdkPrefix${prefix}(non-leaf, core) add private function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_FUNCTION)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, core) add public function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_FUNCTION)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, core) add private class") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_CLASS)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, core) add public class") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_CLASS)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, core) build after error") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    doNotMeasure()
                    expectBuildToFail()
                    changeFile(coreUtilStrings, TypeOfChange.INTRODUCE_COMPILE_ERROR)
                }
                step {
                    changeFile(coreUtilStrings, TypeOfChange.FIX_COMPILE_ERROR)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, core) change popular inline function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(coreUtilCoreLib, TypeOfChange.CHANGE_INLINE_FUNCTION)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, compiler) add public function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(compilerCommonBackendContext, TypeOfChange.ADD_PUBLIC_FUNCTION)
                }
            }

            scenario("$jdkPrefix${prefix}(non-leaf, compiler) add private function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(compilerCommonBackendContext, TypeOfChange.ADD_PRIVATE_FUNCTION)
                }
            }

            scenario("$jdkPrefix${prefix}(leaf, kotlin gradle plugin) add private function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PRIVATE_FUNCTION)
                    runTasks(":kotlin-gradle-plugin:assemble")
                }
            }

            scenario("$jdkPrefix${prefix}(leaf, kotlin gradle plugin) add public function") {
                jdk = jdkPath
                arguments(*nonParallelBuild)
                step {
                    changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PUBLIC_FUNCTION)
                    runTasks(":kotlin-gradle-plugin:assemble")
                }
            }

            scenario("$jdkPrefix${prefix}(leaf, kotlin gradle plugin) measure backup outputs without optimizations") {
                jdk = jdkPath
                arguments(*nonParallelBuild, "-Pkotlin.compiler.preciseCompilationResultsBackup=false", "-Pkotlin.compiler.keepIncrementalCompilationCachesInMemory=false")
                step {
                    changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PRIVATE_FUNCTION)
                    runTasks(":kotlin-gradle-plugin:assemble")
                }
                repeat = 9U
            }

            scenario("$jdkPrefix${prefix}(leaf, kotlin gradle plugin) measure backup outputs with optimizations") {
                jdk = jdkPath
                arguments(*nonParallelBuild, "-Pkotlin.compiler.preciseCompilationResultsBackup=true", "-Pkotlin.compiler.keepIncrementalCompilationCachesInMemory=true")
                step {
                    changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PRIVATE_FUNCTION)
                    runTasks(":kotlin-gradle-plugin:assemble")
                }
                repeat = 9U
            }
        }

        registerScenarios()
        if (withLatestLtsJdk) {
            registerScenarios(withLatestLtsJdk = true)
        }
    }

fun gavra0Benchmarks() =
    suite {
        defaultArguments("-Dorg.gradle.workers.max=8", "--parallel", "--watch-fs")
        defaultJdk = System.getenv("JDK_8")

        val stdlibFileTreeWalk = changeableFile("gavra0/stdlib/FileTreeWalkKt")
        val coreDescriptorsClassDescriptorsBase = changeableFile("gavra0/coreDescriptors/ClassDescriptorBaseJava")

        scenario("build") {
            step {
                doNotMeasure()
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
        }

        scenario("abi change to stdlib") {
            step {
                changeFile(stdlibFileTreeWalk, TypeOfChange.ADD_PUBLIC_FUNCTION)
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
            repeat = 10U
        }

        scenario("abi change to core.descriptors") {
            step {
                changeFile(coreDescriptorsClassDescriptorsBase, TypeOfChange.ADD_PUBLIC_FUNCTION)
                runTasks(Tasks.DIST)
            }
            repeat = 10U
        }
    }