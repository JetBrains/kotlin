import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.gradle.ext.*
import org.jetbrains.kotlin.ideaExt.*
import org.jetbrains.kotlin.buildUtils.idea.*

val ideaPluginDir: File by extra
val ideaSandboxDir: File by extra
val ideaSdkPath: String
    get() = IntellijRootUtils.getIntellijRootDir(rootProject).absolutePath

val intellijUltimateEnabled: Boolean by rootProject.extra
val ideaUltimatePluginDir: File by rootProject.extra
val ideaUltimateSandboxDir: File by rootProject.extra

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

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    allprojects {
        apply(mapOf("plugin" to "idea"))
        afterEvaluate {
            // Make Idea import embedded configuration as transitive dependency
            configurations.findByName("embedded")?.let { embedded ->
                configurations.findByName("runtime")?.extendsFrom(embedded)
            }
        }
    }

    rootProject.afterEvaluate {
        rootProject.allprojects {
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

                        ideaPlugin()
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
                        fun idea(
                            title: String,
                            sandboxDir: File,
                            pluginDir: File
                        ) {
                            application(title) {
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
                                    "-Didea.system.path=${sandboxDir.absolutePath}",
                                    "-Didea.config.path=${sandboxDir.absolutePath}/config",
                                    "-Dapple.laf.useScreenMenuBar=true",
                                    "-Dapple.awt.graphics.UseQuartz=true",
                                    "-Dsun.io.useCanonCaches=false",
                                    "-Dplugin.path=${pluginDir.absolutePath}"
                                ).joinToString(" ")
                            }
                        }

                        idea("[JPS] IDEA", ideaSandboxDir, ideaPluginDir)

                        if (intellijUltimateEnabled) {
                            idea("[JPS] IDEA Ultimate", ideaUltimateSandboxDir, ideaPluginDir)
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

                        if (intellijUltimateEnabled) {
                            junit("[JPS] All IDEA Ultimate Plugin Tests") {
                                moduleName = "kotlin.ultimate.test"
                                pattern = "org.jetbrains.kotlin.*"
                                configureForKotlin()
                            }
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

fun NamedDomainObjectContainer<TopLevelArtifact>.ideaPlugin() {
    val ideaPluginProject = project(":prepare:idea-plugin")
    (ideaPluginProject as ProjectInternal).evaluate()
    val libraries by ideaPluginProject.configurations
    val jpsPlugin by ideaPluginProject.configurations

    create("ideaPlugin") {
        directory("Kotlin") {
            directory("kotlinc") {
                artifact("kotlinc")
            }

            directory("lib") {
                archive("kotlin-plugin.jar") {
                    directory("META-INF") {
                        file("$buildDir/tmp/jar/MANIFEST.MF")
                    }

                    file("${ideaPluginProject.rootDir}/resources/kotlinManifest.properties")
                    
                    jarFromEmbedded(ideaPluginProject)
                }

                directoryFromConfiguration(libraries)

                directory("jps") {
                    directoryFromConfiguration(jpsPlugin)
                }
            }
        }
    }
}

val jarArtifactProjects = listOf(
    "kotlin-compiler-client-embeddable",
    "kotlin-compiler",
    "kotlin-daemon-client",
    "kotlin-imports-dumper-compiler-plugin",
    "kotlin-jps-plugin",
    "kotlin-main-kts",
    "kotlin-reflect"
)

fun moduleName(projectPath: String) = rootProject.name + projectPath.replace(':', '.') + ".main"

fun RecursiveArtifact.jarFromEmbedded(project: Project) {
    val embedded = project.configurations.findByName("embedded") ?: return

    val resolvedArtifacts = embedded
        .resolvedConfiguration
        .resolvedArtifacts

    resolvedArtifacts.filter { it.id.componentIdentifier is ModuleComponentIdentifier }
        .map { it.file }
        .forEach(::extractedDirectory)

    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            moduleOutput(moduleName(it.projectPath))
            jarFromEmbedded(project(it.projectPath))
        }
}

fun RecursiveArtifact.directoryFromConfiguration(configuration: Configuration) {
    val resolvedArtifacts = configuration
        .resolvedConfiguration
        .resolvedArtifacts

    resolvedArtifacts.filter { it.id.componentIdentifier is ModuleComponentIdentifier }
        .map { it.file }
        .forEach(::file)

    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            val artifactName = it.projectName + ".jar"
            if (it.projectName in jarArtifactProjects) {
                artifact(artifactName)
            } else {
                archive(artifactName) {
                    moduleOutput(moduleName(it.projectPath))
                }
            }
        }
}