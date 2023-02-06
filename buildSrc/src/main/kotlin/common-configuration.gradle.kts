import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper

// Contains common configuration that should be applied to all projects

// Common Group and version
val kotlinVersion: String by rootProject.extra
group = "org.jetbrains.kotlin"
version = kotlinVersion

// Forcing minimal gson dependency version
val gsonVersion = rootProject.extra["versions.gson"] as String
dependencies {
    constraints {
        configurations.all {
            if (isCanBeResolved) {
                allDependencies.configureEach {
                    if (group == "com.google.code.gson" && name == "gson") {
                        this@constraints.add(this@all.name, "com.google.code.gson:gson") {
                            version {
                                require(gsonVersion)
                            }
                            because("Force using same gson version because of https://github.com/google/gson/pull/1991")
                        }
                    }
                }
            }
        }
    }
}

project.configureJvmDefaultToolchain()
project.addEmbeddedConfigurations()
project.configureJavaCompile()
project.configureJavaBasePlugin()
project.configureKotlinCompilationOptions()
project.configureArtifacts()
project.configureTests()

// There are problems with common build dir:
//  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
//  - idea seems unable to exclude common buildDir from indexing
// therefore it is disabled by default
// buildDir = File(commonBuildDir, project.name)

afterEvaluate {
    run configureCompilerClasspath@{
        val bootstrapCompilerClasspath by rootProject.buildscript.configurations
        configurations.findByName("kotlinCompilerClasspath")?.let {
            dependencies.add(it.name, files(bootstrapCompilerClasspath))
        }

        configurations.findByName("kotlinCompilerPluginClasspath")
            ?.exclude("org.jetbrains.kotlin", "kotlin-scripting-compiler-embeddable")
    }
}

fun Project.addEmbeddedConfigurations() {
    configurations.maybeCreate("embedded").apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }

    configurations.maybeCreate("embeddedElements").apply {
        extendsFrom(configurations["embedded"])
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named("embedded-java-runtime"))
        }
    }
}

fun Project.configureJavaCompile() {
    plugins.withType<JavaPlugin> {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-Xlint:deprecation")
            options.compilerArgs.add("-Xlint:unchecked")
            options.compilerArgs.add("-Werror")
        }
    }
}

fun Project.configureJavaBasePlugin() {
    plugins.withId("java-base") {
        fun File.toProjectRootRelativePathOrSelf() = (relativeToOrNull(rootDir)?.takeUnless { it.startsWith("..") } ?: this).path

        fun FileCollection.printClassPath(role: String) =
            println("${project.path} $role classpath:\n  ${joinToString("\n  ") { it.toProjectRootRelativePathOrSelf() }}")

        val javaExtension = javaPluginExtension()
        tasks {
            register("printCompileClasspath") { doFirst { javaExtension.sourceSets["main"].compileClasspath.printClassPath("compile") } }
            register("printRuntimeClasspath") { doFirst { javaExtension.sourceSets["main"].runtimeClasspath.printClassPath("runtime") } }
            register("printTestCompileClasspath") { doFirst { javaExtension.sourceSets["test"].compileClasspath.printClassPath("test compile") } }
            register("printTestRuntimeClasspath") { doFirst { javaExtension.sourceSets["test"].runtimeClasspath.printClassPath("test runtime") } }
        }
    }
}

fun Project.configureKotlinCompilationOptions() {
    plugins.withType<KotlinBasePluginWrapper> {
        val commonCompilerArgs = listOfNotNull(
            "-opt-in=kotlin.RequiresOptIn",
            "-progressive".takeIf { getBooleanProperty("test.progressive.mode") ?: false }
        )

        val kotlinLanguageVersion: String by rootProject.extra
        val useJvmFir by extra(project.kotlinBuildProperties.useFir)
        val useFirLT by extra(project.kotlinBuildProperties.useFirWithLightTree)
        val useFirIC by extra(project.kotlinBuildProperties.useFirTightIC)
        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)

        @Suppress("DEPRECATION")
        tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
            kotlinOptions {
                languageVersion = kotlinLanguageVersion
                apiVersion = kotlinLanguageVersion
                freeCompilerArgs += commonCompilerArgs
            }

            val relativePathBaseArg: String? =
                "-Xklib-relative-path-base=$buildDir,$projectDir,$rootDir".takeIf {
                    !kotlinBuildProperties.getBoolean("kotlin.build.use.absolute.paths.in.klib")
                }

            // Workaround to avoid remote build cache misses due to absolute paths in relativePathBaseArg
            doFirst {
                if (relativePathBaseArg != null) {
                    @Suppress("DEPRECATION")
                    kotlinOptions.freeCompilerArgs += relativePathBaseArg
                }
            }
        }

        val jvmCompilerArgs = listOf(
            "-Xno-optimized-callable-references",
            "-Xno-kotlin-nothing-value-exception",
        )

        val coreLibProjects: List<String> by rootProject.extra
        val projectsWithDisabledFirBootstrap = coreLibProjects + listOf(
            ":kotlin-gradle-plugin",
            ":kotlinx-metadata",
            ":kotlinx-metadata-jvm",
            // For some reason stdlib isn't imported correctly for this module
            // Probably it's related to kotlin-test module usage
            ":kotlin-gradle-statistics",
            // Requires serialization plugin
            ":wasm:wasm.ir",
            // Uses multiplatform
            ":kotlin-stdlib-jvm-minimal-for-test",
            ":kotlin-native:endorsedLibraries:kotlinx.cli",
            ":kotlin-native:klib",
            // Requires serialization plugin
            ":js:js.tests",
        )

        // TODO: fix remaining warnings and remove this property.
        val tasksWithWarnings = listOf(
            ":kotlin-gradle-plugin:compileCommonKotlin",
            ":kotlin-native:build-tools:compileKotlin"
        )

        val projectsWithEnabledContextReceivers: List<String> by rootProject.extra
        val projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib: List<String> by rootProject.extra

        @Suppress("SuspiciousCollectionReassignment", "DEPRECATION")
        tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile>().configureEach {
            kotlinOptions {
                freeCompilerArgs += jvmCompilerArgs

                if (useJvmFir) {
                    if (project.path !in projectsWithDisabledFirBootstrap) {
                        freeCompilerArgs += "-Xuse-k2"
                        freeCompilerArgs += "-Xabi-stability=stable"
                        if (useFirLT) {
                            freeCompilerArgs += "-Xuse-fir-lt"
                        }
                        if (useFirIC) {
                            freeCompilerArgs += "-Xuse-fir-ic"
                        }
                    } else {
                        freeCompilerArgs += "-Xskip-prerelease-check"
                    }
                }
                if (renderDiagnosticNames) {
                    freeCompilerArgs += "-Xrender-internal-diagnostic-names"
                }
                if (path !in tasksWithWarnings) {
                    allWarningsAsErrors = !kotlinBuildProperties.disableWerror
                }
                if (project.path in projectsWithEnabledContextReceivers) {
                    freeCompilerArgs += "-Xcontext-receivers"
                }
                if (project.path in projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib) {
                    freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction"
                }

                if (project.path == ":kotlin-util-klib") {
                    // This is a temporary workaround for a configuration problem in kotlin-native. Namely, module `:kotlin-native-shared`
                    // depends on kotlin-util-klib from bootstrap for some reason (see `kotlin-native/shared/build.gradle.kts`), but when
                    // we're packing dependencies for the use in the IDE, we pass paths to the newly built libraries to Proguard
                    // (see `prepare/ide-plugin-dependencies/kotlin-backend-native-for-ide/build.gradle.kts`).
                    //
                    // So the code which was compiled against one version of a library, is analyzed by Proguard against another version.
                    //
                    // This is a bad situation for JVM default flag behavior specifically. If kotlin-util-klib from bootstrap is compiled
                    // in the old mode (with DefaultImpls for interfaces), then subclasses in kotlin-native-shared will also be generated
                    // in the old mode (with DefaultImpls). But then Proguard will analyze these subclasses and their DefaultImpls classes,
                    // and will observe calls to non-existing methods from DefaultImpls of the interfaces in kotlin-util-klib, and report
                    // an error.
                    //
                    // This change will most likely not be needed after the bootstrap, as soon as kotlin-util-klib is compiled with
                    // `-Xjvm-default=all`.
                    freeCompilerArgs += "-Xjvm-default=all-compatibility"
                } else if (!skipJvmDefaultAllForModule(project.path)) {
                    freeCompilerArgs += "-Xjvm-default=all"
                }
            }
        }
    }
}

fun Project.configureArtifacts() {
    tasks.withType<Javadoc>().configureEach {
        enabled = false
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        val `rw-r--r--` = 0b110100100
        val `rwxr-xr-x` = 0b111101101
        fileMode = `rw-r--r--`
        dirMode = `rwxr-xr-x`
        filesMatching("**/bin/*") { mode = `rwxr-xr-x` }
        filesMatching("**/bin/*.bat") { mode = `rw-r--r--` }
    }

    normalization {
        runtimeClasspath {
            ignore("META-INF/MANIFEST.MF")
            ignore("META-INF/compiler.version")
            ignore("META-INF/plugin.xml")
            ignore("kotlin/KotlinVersionCurrentValue.class")
        }
    }

    fun Task.listConfigurationContents(configName: String) {
        doFirst {
            project.configurations.findByName(configName)?.let {
                println("$configName configuration files:\n${it.allArtifacts.files.files.joinToString("\n  ", "  ")}")
            }
        }
    }

    tasks.register("listArchives") { listConfigurationContents("archives") }
    tasks.register("listDistJar") { listConfigurationContents("distJar") }
}

fun Project.configureTests() {
    val ignoreTestFailures: Boolean by rootProject.extra
    tasks.configureEach {
        if (this is VerificationTask) {
            ignoreFailures = ignoreTestFailures
        }
    }

    tasks.withType<Test>().configureEach {
        outputs.doNotCacheIf("https://youtrack.jetbrains.com/issue/KTI-112") { true }
    }

    // Aggregate task for build related checks
    tasks.register("checkBuild")

    afterEvaluate {
        apply(from = "$rootDir/gradle/testRetry.gradle.kts")
    }
}

// TODO: migrate remaining modules to the new JVM default scheme.
fun skipJvmDefaultAllForModule(path: String): Boolean =
// Gradle plugin modules are disabled because different Gradle versions bundle different Kotlin compilers,
    // and not all of them support the new JVM default scheme.
    "-gradle" in path || "-runtime" in path || path == ":kotlin-project-model" ||
            // Visitor/transformer interfaces in ir.tree are very sensitive to the way interface methods are implemented.
            // Enabling default method generation results in a performance loss of several % on full pipeline test on Kotlin.
            // TODO: investigate the performance difference and enable new mode for ir.tree.
            path == ":compiler:ir.tree" ||
            // Workaround a Proguard issue:
            //     java.lang.IllegalAccessError: tried to access method kotlin.reflect.jvm.internal.impl.types.checker.ClassicTypeSystemContext$substitutionSupertypePolicy$2.<init>(
            //       Lkotlin/reflect/jvm/internal/impl/types/checker/ClassicTypeSystemContext;Lkotlin/reflect/jvm/internal/impl/types/TypeSubstitutor;
            //     )V from class kotlin.reflect.jvm.internal.impl.resolve.OverridingUtilTypeSystemContext
            // KT-54749
            path == ":core:descriptors"
