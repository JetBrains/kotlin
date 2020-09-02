import org.jetbrains.kotlin.build.benchmarks.dsl.*

fun kotlinBenchmarks() =
    suite {
        val coreUtilStrings = changeableFile("coreUtil/StringsKt")
        val coreUtilCoreLib = changeableFile("coreUtil/CoreLibKt")
        val compilerCommonBackendContext = changeableFile("compiler/CommonBackendContext")
        val kotlinGradlePluginConfigurationPhaseAware = changeableFile("kotlinGradlePlugin/ConfigurationPhaseAware")

        defaultTasks(Tasks.DIST, Tasks.COMPILER_TEST_CLASSES, Tasks.IDEA_TEST_CLASSES, Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
        defaultJdk = System.getenv("JDK_8")

        val defaultArguments = listOf(
            "--info",
            "--no-build-cache",
            "--watch-fs",
        )

        defaultArguments(*defaultArguments.toTypedArray())

        scenario("clean build") {
            arguments(
                *defaultArguments.toTypedArray(),
            )
            expectSlowBuild("clean build")
            step {
                doNotMeasure()
                runTasks(Tasks.CLEAN)
            }
            step {}
        }

        scenario("clean build parallel") {
            arguments(
                *defaultArguments.toTypedArray(),
                "--parallel"
            )
            expectSlowBuild("clean build")
            step {
                doNotMeasure()
                runTasks(Tasks.CLEAN)
            }
            step {}
        }

        scenario("(non-leaf, core) add private function") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("(non-leaf, core) add public function") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("(non-leaf, core) add private class") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_CLASS)
            }
        }

        scenario("(non-leaf, core) add public class") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_CLASS)
            }
        }

        scenario("(non-leaf, core) build after error") {
            step {
                doNotMeasure()
                expectBuildToFail()
                changeFile(coreUtilStrings, TypeOfChange.INTRODUCE_COMPILE_ERROR)
            }
            step {
                changeFile(coreUtilStrings, TypeOfChange.FIX_COMPILE_ERROR)
            }
        }

        scenario("(non-leaf, core) change popular inline function") {
            step {
                changeFile(coreUtilCoreLib, TypeOfChange.CHANGE_INLINE_FUNCTION)
            }
        }

        scenario("(non-leaf, compiler) add public function") {
            step {
                changeFile(compilerCommonBackendContext, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("(non-leaf, compiler) add private function") {
            step {
                changeFile(compilerCommonBackendContext, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("(leaf, kotlin gradle plugin) add private function") {
            step {
                changeFile(kotlinGradlePluginConfigurationPhaseAware, TypeOfChange.ADD_PRIVATE_FUNCTION)
                runTasks(Tasks.KOTLIN_GRADLE_PLUGIN_COMPILE_JAVA)
            }
        }

        scenario("(leaf, kotlin gradle plugin) add public function") {
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
                runTasks(Tasks.DIST, Tasks.IDEA_PLUGIN)
            }
            repeat = 10U
        }
    }