import org.jetbrains.gradle.ext.*
import org.jetbrains.kotlin.ideaExt.*
import org.jetbrains.kotlin.buildUtils.idea.*

val isJpsBuildEnabled = findProperty("jpsBuild")?.toString() == "true"
val ideaPluginDir: File by extra
val ideaSandboxDir: File by extra
val ideaSdkPath: String
    get() = IntellijRootUtils.getIntellijRootDir(rootProject).absolutePath

fun org.jetbrains.gradle.ext.JUnit.configureForKotlin() {
    vmParameters = listOf(
        "-ea",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-Xmx1600m",
        "-XX:+UseCodeCacheFlushing",
        "-XX:ReservedCodeCacheSize=128m",
        "-Djna.nosys=true",
        "-Didea.is.unit.test=true",
        "-Didea.home.path=$ideaSdkPath",
        "-Djps.kotlin.home=${ideaPluginDir.absolutePath}",
        "-Dkotlin.ni=" + if (rootProject.hasProperty("newInferenceTests")) "true" else "false",
        "-Duse.jps=true"
    ).joinToString(" ")
    envs = mapOf(
        "NO_FS_ROOTS_ACCESS_CHECK" to "true",
        "PROJECT_CLASSES_DIRS" to "out/test/org.jetbrains.kotlin.compiler.test"
    )
    workingDirectory = rootDir.toString()
}

if (isJpsBuildEnabled && System.getProperty("idea.active") != null) {
    allprojects {
        apply(mapOf("plugin" to "idea"))
    }

    rootProject.afterEvaluate {
        allprojects {
            idea {
                module {
                    inheritOutputDirs = true
                }
            }
        }

        rootProject.idea {
            project {
                settings {
                    ideArtifacts {
                        generateIdeArtifacts(rootProject, this@ideArtifacts)
                    }

                    compiler {
                        processHeapSize = 2000
                        addNotNullAssertions = true
                        parallelCompilation = true
                    }

                    delegateActions {
                        delegateBuildRunToGradle = false
                        testRunner = ActionDelegationConfig.TestRunner.PLATFORM
                    }

                    runConfigurations {
                        application("[JPS] IDEA") {
                            moduleName = "kotlin.idea-runner.main"
                            workingDirectory = File(intellijRootDir(), "bin").toString()
                            mainClass = "com.intellij.idea.Main"
                            jvmArgs = listOf(
                                "-Xmx1250m",
                                "-XX:ReservedCodeCacheSize=240m",
                                "-XX:+HeapDumpOnOutOfMemoryError",
                                "-ea",
                                "-Didea.is.internal=true",
                                "-Didea.debug.mode=true",
                                "-Didea.system.path=${ideaSandboxDir.absolutePath}",
                                "-Didea.config.path=${ideaSandboxDir.absolutePath}/config",
                                "-Dapple.laf.useScreenMenuBar=true",
                                "-Dapple.awt.graphics.UseQuartz=true",
                                "-Dsun.io.useCanonCaches=false",
                                "-Dplugin.path=${ideaPluginDir.absolutePath}"
                            ).joinToString(" ")
                        }

                        application("[JPS] Generate All Tests") {
                            moduleName = "kotlin.pill.generate-all-tests.test"
                            workingDirectory = rootDir.toString()
                            mainClass = "org.jetbrains.kotlin.pill.generateAllTests.Main"
                        }

                        defaults<org.jetbrains.gradle.ext.JUnit> {
                            configureForKotlin()
                        }

                        // todo: replace `pattern` with `package`, when `com.intellij.execution.junit.JUnitRunConfigurationImporter#process` will be fixed
                        junit("[JPS] All IDEA Plugin Tests") {
                            moduleName = "kotlin.idea.test"
                            pattern = "org.jetbrains.kotlin.*"
                            configureForKotlin()
                        }

                        junit("[JPS] Compiler Tests") {
                            moduleName = "kotlin.compiler.test"
                            pattern = "org.jetbrains.kotlin.*"
                            configureForKotlin()
                        }

                        junit("[JPS] JVM Backend Tests") {
                            moduleName = "kotlin.idea.test"
                            pattern = "org.jetbrains.kotlin.codegen.*"
                            configureForKotlin()
                        }

                        junit("[JPS] JS Backend Tests") {
                            moduleName = "kotlin.js.js.tests.test"
                            pattern = "org.jetbrains.kotlin.js.test.*"
                            configureForKotlin()
                        }

                        junit("[JPS] Java 8 Tests") {
                            moduleName = "kotlin.compiler.tests-java8.test"
                            pattern = "org.jetbrains.kotlin.*"
                            configureForKotlin()
                        }
                    }
                }
            }
        }
    }
}