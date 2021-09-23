import org.jetbrains.kotlin.build.benchmarks.dsl.*

fun abiSnapshotBenchmarks() = kotlinBenchmarks(prefix = "ABI_SNAPSHOT: ", arrayOf("-Dkotlin.incremental.classpath.snapshot.enabled=true"))
fun artifactTransformBenchmarks() = kotlinBenchmarks(prefix = "TRANSFORMATION: ", arrayOf("-Dkotlin.incremental.useClasspathSnapshot=true"))

//move prefix to suite
fun kotlinBenchmarks(prefix: String = "", additionalDefaultProperties: Array<String> = emptyArray()) =
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
        )

        val nonParallelRerunBuild = arrayOf(
            *nonParallelBuild,
            "--rerun-tasks",
        )

        defaultArguments(*defaultArguments)

        scenario("${prefix}clean build") {
            arguments(*nonParallelRerunBuild)
            expectSlowBuild("clean build")
            step {
                doNotMeasure()
                runTasks(Tasks.CLEAN)
            }
            step {}
        }

        scenario("${prefix}(buildSrc, Kotlin) add public fun") {
            step {
                changeFile(buildSrc, TypeOfChange.ADD_PUBLIC_FUNCTION)
                runTasks(*noArgs)
            }
        }

        scenario("${prefix}(buildSrc, Kotlin) add private fun") {
            step {
                changeFile(buildSrc, TypeOfChange.ADD_PRIVATE_FUNCTION)
                runTasks(*noArgs)
            }
        }

        scenario("${prefix}clean build parallel") {
            arguments(*parallelRerunBuild)
            expectSlowBuild("clean build")
            step {
                doNotMeasure()
                runTasks(Tasks.CLEAN)
            }
            step {}
        }

        scenario("${prefix}Run gradle plugin tests") {
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

        scenario("${prefix}Run gradle plugin tests after changes") {
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

        scenario("${prefix}(non-leaf, core) add private function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("${prefix}(non-leaf, core) add public function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("${prefix}(non-leaf, core) add private class") {
            arguments(*nonParallelBuild)
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_CLASS)
            }
        }

        scenario("${prefix}(non-leaf, core) add public class") {
            arguments(*nonParallelBuild)
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_CLASS)
            }
        }

        scenario("${prefix}(non-leaf, core) build after error") {
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

        scenario("${prefix}(non-leaf, core) change popular inline function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(coreUtilCoreLib, TypeOfChange.CHANGE_INLINE_FUNCTION)
            }
        }

        scenario("${prefix}(non-leaf, compiler) add public function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(compilerCommonBackendContext, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("${prefix}(non-leaf, compiler) add private function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(compilerCommonBackendContext, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("${prefix}(leaf, kotlin gradle plugin) add private function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PRIVATE_FUNCTION)
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
        }

        scenario("${prefix}(leaf, kotlin gradle plugin) add public function") {
            arguments(*nonParallelBuild)
            step {
                changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PUBLIC_FUNCTION)
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
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