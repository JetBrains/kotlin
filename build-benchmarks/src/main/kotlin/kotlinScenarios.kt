import org.jetbrains.kotlin.build.benchmarks.dsl.*

fun kotlinBenchmarks() =
    suite {
        val coreUtilStrings = changeableFile("coreUtil/StringsKt")
        val coreUtilCoreLib = changeableFile("coreUtil/CoreLibKt")

        defaultTasks(Tasks.DIST, Tasks.COMPILER_TEST_CLASSES, Tasks.IDEA_TEST_CLASSES)
        defaultJdk = System.getenv("JDK_8")

        scenario("clean build") {
            expectSlowBuild("clean build")
            step {
                doNotMeasure()
                runTasks(Tasks.CLEAN)
            }
            step {}
        }

        scenario("add private function") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_FUNCTION)
            }
        }

        scenario("add public function") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_FUNCTION)
            }
        }

        scenario("add private class") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PRIVATE_CLASS)
            }
        }

        scenario("add public class") {
            step {
                changeFile(coreUtilStrings, TypeOfChange.ADD_PUBLIC_CLASS)
            }
        }

        scenario("build after error") {
            step {
                doNotMeasure()
                expectBuildToFail()
                changeFile(coreUtilStrings, TypeOfChange.INTRODUCE_COMPILE_ERROR)
            }
            step {
                changeFile(coreUtilStrings, TypeOfChange.FIX_COMPILE_ERROR)
            }
        }

        scenario("change popular inline function") {
            step {
                changeFile(coreUtilCoreLib, TypeOfChange.CHANGE_INLINE_FUNCTION)
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