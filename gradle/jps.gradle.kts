@file:Suppress("UnstableApiUsage")

import org.gradle.jvm.tasks.Jar
import org.jetbrains.gradle.ext.*
import org.jetbrains.kotlin.ideaExt.*


val ideaPluginDir: File by extra
val ideaSandboxDir: File by extra
val ideaSdkPath: String
    get() = IntellijRootUtils.getIntellijRootDir(rootProject).absolutePath

val intellijUltimateEnabled: Boolean by rootProject.extra
val ideaUltimatePluginDir: File by rootProject.extra
val ideaUltimateSandboxDir: File by rootProject.extra

fun JUnit.configureForKotlin() {
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
        "-Duse.jps=true",
        "-Djava.awt.headless=true"
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
        // Make Idea import embedded configuration as transitive dependency for some configurations
        afterEvaluate {
            listOf(
                "testCompile",
                "testCompileOnly",
                "testRuntime",
                "testRuntimeOnly"
            ).forEach { configurationName ->
                val dependencyProjects = configurations
                    .findByName(configurationName)
                    ?.dependencies
                    ?.mapNotNull { (it as? ProjectDependency)?.dependencyProject }

                dependencies {
                    dependencyProjects?.forEach {dependencyProject ->
                        add(configurationName, project(dependencyProject.path, configuration = "embedded"))
                    }
                }
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
            
            if (this != rootProject) {
                evaluationDependsOn(path)
            }
        }

        rootProject.idea {
            project {
                settings {
                    ideArtifacts {
                        kotlinCompilerJar()
                        
                        kotlinPluginJar()

                        kotlinReflectJar()

                        kotlinCompilerClientEmbeddableJar()

                        kotlinMainKtsJar()

                        kotlinImportsDumperCompilerPluginJar()

                        kotlinDaemonClientJar()

                        kotlinJpsPluginJar()

                        kotlinc()

                        ideaPlugin()

                        dist()
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
                            pluginDir: File,
                            disableProcessCanceledException: Boolean = false
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
                                    "-Dplugin.path=${pluginDir.absolutePath}",
                                    "-Didea.ProcessCanceledException=${if (disableProcessCanceledException) "disabled" else "enabled"}"
                                ).joinToString(" ")
                            }
                        }

                        idea("[JPS] IDEA", ideaSandboxDir, ideaPluginDir)

                        idea("[JPS] IDEA (No ProcessCanceledException)", ideaSandboxDir, ideaPluginDir, disableProcessCanceledException = true)

                        if (intellijUltimateEnabled) {
                            idea("[JPS] IDEA Ultimate", ideaUltimateSandboxDir, ideaPluginDir)
                        }

                        application("[JPS] Generate All Tests") {
                            moduleName = "kotlin.pill.generate-all-tests.test"
                            workingDirectory = rootDir.toString()
                            mainClass = "org.jetbrains.kotlin.pill.generateAllTests.Main"
                        }

                        defaults<JUnit> {
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

val jarArtifactProjects = listOf(
    "kotlin-compiler-client-embeddable",
    "kotlin-compiler",
    "kotlin-daemon-client",
    "kotlin-imports-dumper-compiler-plugin",
    "kotlin-jps-plugin",
    "kotlin-main-kts",
    "kotlin-reflect"
)

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinCompilerJar() =
    jarFromProject(project(":kotlin-compiler"), "kotlin-compiler.jar")

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinReflectJar() =
    jarFromProject(project(":kotlin-reflect"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinCompilerClientEmbeddableJar() =
    jarFromProject(project(":kotlin-compiler-client-embeddable"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinMainKtsJar() {
    val mainKtsProject = project(":kotlin-main-kts")
    jarFromProject(mainKtsProject) {
        directoryContent("${mainKtsProject.rootDir}/jar-resources")
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinPluginJar() =
    jarFromProject(project(":prepare:idea-plugin"), "kotlin-plugin.jar")

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinImportsDumperCompilerPluginJar() =
    jarFromProject(project(":kotlin-imports-dumper-compiler-plugin"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinDaemonClientJar() =
    jarFromProject(project(":kotlin-daemon-client"))

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinJpsPluginJar() {
    val jpsPluginProject = project(":kotlin-jps-plugin")
    jarFromProject(jpsPluginProject) {
        file("${jpsPluginProject.rootDir}/resources/kotlinManifest.properties")
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.dist() {
    val distLibrariesProject = project(":kotlin-stdlib:jps-build")
    val stdlibMinimal by distLibrariesProject.configurations
    val commonStdlib by distLibrariesProject.configurations
    val commonStdlibSources by distLibrariesProject.configurations
    val stdlibJS by distLibrariesProject.configurations
    val stdlibSources by distLibrariesProject.configurations

    create("dist") {
        file("$rootDir/build/build.txt")

        // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
        archive("kotlin-stdlib-minimal-for-test.jar") {
            extractedDirectory(stdlibMinimal.singleFile)
        }

        directory("artifacts") {
            directory("ideaPlugin") {
                artifact("ideaPlugin")
            }
        }

        directory("common") {
            // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
            archive("kotlin-stdlib-common.jar") {
                extractedDirectory(commonStdlib.singleFile)
            }

            // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
            archive("kotlin-stdlib-common-sources.jar") {
                extractedDirectory(commonStdlibSources.singleFile)
            }
        }

        directory("js") {
            extractedDirectory(stdlibJS.singleFile)
        }

        directory("kotlinc") {
            artifact("kotlinc")
        }

        directory("maven") {
            // Use output-file-name when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
            archive("kotlin-stdlib-sources.jar") {
                extractedDirectory(stdlibSources.singleFile)
            }
        }
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.kotlinc() {
    val kotlinCompilerProject = project(":kotlin-compiler")
    val libraries by kotlinCompilerProject.configurations
    val compilerPlugins by kotlinCompilerProject.configurations
    val sources by kotlinCompilerProject.configurations

    create("kotlinc") {
        directory("bin") {
            directoryContent("$rootDir/compiler/cli/bin")
        }

        directory("lib") {
            artifact("kotlin-compiler.jar")
            jarsFromConfiguration(libraries) { it.replace("-$bootstrapKotlinVersion", "") }
            jarsFromConfiguration(compilerPlugins) { it.removePrefix("kotlin-") }
            sourceJarsFromConfiguration(sources) { it.replace("-$bootstrapKotlinVersion", "") }
        }

        directory("license") {
            directoryContent("$rootDir/license")
        }
        
        file("$rootDir/bootstrap/build.txt")
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.ideaPlugin() {
    val ideaPluginProject = project(":prepare:idea-plugin")
    val libraries by ideaPluginProject.configurations
    val jpsPlugin by ideaPluginProject.configurations

    create("ideaPlugin") {
        directory("Kotlin") {
            directory("kotlinc") {
                artifact("kotlinc")
            }

            directory("lib") {
                artifact("kotlin-plugin.jar")

                jarsFromConfiguration(libraries) { it.replace("-$bootstrapKotlinVersion", "") }

                directory("jps") {
                    jarsFromConfiguration(jpsPlugin)
                }
            }
        }
    }
}

fun NamedDomainObjectContainer<TopLevelArtifact>.jarFromProject(project: Project, name: String? = null, configureAction: RecursiveArtifact.() -> Unit = {}) {
    val jarName = name ?: project.name + ".jar"
    create(jarName) {
        archiveFromProject(project, jarName, configureAction)
    }
}

fun RecursiveArtifact.archiveFromProject(project: Project, name: String? = null, configureAction: RecursiveArtifact.() -> Unit = {}) {
    val jarName = name ?: project.name + ".jar"
    archive(jarName) {
        (project.tasks["jar"] as? Jar)?.let { jar ->
            val manifestPath = jar.temporaryDir.resolve("MANIFEST.MF")
            jar.manifest.writeTo(manifestPath)
            directory("META-INF") {
                file(manifestPath)
            }
        }

        if (project.sourceSets.names.contains("main")) {
            moduleOutput(moduleName(project.path))
        }

        jarContentsFromEmbeddedConfiguration(project)

        configureAction()
    }
}

fun moduleName(projectPath: String) = rootProject.name + projectPath.replace(':', '.') + ".main"

fun RecursiveArtifact.jarContentsFromEmbeddedConfiguration(project: Project) {
    val embedded = project.configurations.findByName("embedded") ?: return
    jarContentsFromConfiguration(embedded)
}

fun RecursiveArtifact.jarContentsFromConfiguration(configuration: Configuration) {
    val resolvedArtifacts = configuration
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
            jarContentsFromEmbeddedConfiguration(project(it.projectPath))
        }
}

fun RecursiveArtifact.sourceJarsFromConfiguration(configuration: Configuration, renamer: (String) -> String = { it }) {
    val resolvedArtifacts = configuration
        .resolvedConfiguration
        .resolvedArtifacts

    jarsFromExternalModules(resolvedArtifacts, renamer)
    
    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            val jarBaseName = project(it.projectPath).the<BasePluginConvention>().archivesBaseName
            val renamed = renamer("$jarBaseName-sources") + ".jar"
            archive(renamed) {
                project(it.projectPath)
                    .mainSourceSet
                    .allSource
                    .sourceDirectories
                    .forEach {sourceDirectory ->
                    directoryContent(sourceDirectory)
                }
            }
        }
}

fun RecursiveArtifact.jarsFromConfiguration(configuration: Configuration, renamer: (String) -> String = { it }) {
    val resolvedArtifacts = configuration
        .resolvedConfiguration
        .resolvedArtifacts

    jarsFromExternalModules(resolvedArtifacts, renamer)

    resolvedArtifacts
        .map { it.id.componentIdentifier }
        .filterIsInstance<ProjectComponentIdentifier>()
        .forEach {
            val jarBaseName = project(it.projectPath).the<BasePluginConvention>().archivesBaseName
            val artifactName = renamer(jarBaseName) + ".jar"
            if (it.projectName in jarArtifactProjects) {
                artifact(artifactName)
            } else {
                archiveFromProject(project(it.projectPath), artifactName)
            }
        }
}

fun RecursiveArtifact.jarsFromExternalModules(resolvedArtifacts: Iterable<ResolvedArtifact>, renamer: (String) -> String = { it }) {
    // Use output-file-name property when fixed https://github.com/JetBrains/gradle-idea-ext-plugin/issues/63
    resolvedArtifacts.filter { it.id.componentIdentifier is ModuleComponentIdentifier }
        .forEach {
            val baseName = it.file.nameWithoutExtension
            val renamed = renamer(baseName)
            if (it.file.extension == "jar" && renamed != baseName) {
                archive("$renamed.jar") {
                    extractedDirectory(it.file)
                }
            } else {
                file(it.file)
            }
        }
}