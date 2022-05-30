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
            this@constraints.add(name, "com.google.code.gson:gson") {
                version {
                    require(gsonVersion)
                }
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }
}

project.applyFixForStdlib16()

apply(from = "$rootDir/gradle/cacheRedirector.gradle.kts")
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
        project.configureShadowJarSubstitutionInCompileClasspath()

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
            "-progressive".takeIf { hasProperty("test.progressive.mode") }
        )

        val kotlinLanguageVersion: String by rootProject.extra
        val useJvmFir by extra(project.kotlinBuildProperties.useFir)
        val useFirLT by extra(project.kotlinBuildProperties.useFirWithLightTree)
        val useFirIC by extra(project.kotlinBuildProperties.useFirTightIC)
        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)

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

            doFirst {
                if (relativePathBaseArg != null) {
                    kotlinOptions.freeCompilerArgs += relativePathBaseArg
                }
            }
        }

        val jvmCompilerArgs = listOf(
            "-Xno-optimized-callable-references",
            "-Xno-kotlin-nothing-value-exception",
            "-Xsuppress-deprecated-jvm-target-warning" // Remove as soon as there are no modules for JDK 1.6 & 1.7
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
            ":wasm:wasm.ir"
        )

        // TODO: fix remaining warnings and remove this property.
        val tasksWithWarnings = listOf(
            ":kotlin-gradle-plugin:compileCommonKotlin",
            ":kotlin-gradle-plugin:compileGradle70Kotlin"
        )

        val projectsWithEnabledContextReceivers: List<String> by rootProject.extra

        @Suppress("SuspiciousCollectionReassignment", "DEPRECATION")
        tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile>().configureEach {
            kotlinOptions {
                freeCompilerArgs += jvmCompilerArgs

                if (useJvmFir && project.path !in projectsWithDisabledFirBootstrap) {
                    freeCompilerArgs += "-Xuse-fir"
                    freeCompilerArgs += "-Xabi-stability=stable"
                    if (useFirLT) {
                        freeCompilerArgs += "-Xuse-fir-lt"
                    }
                    if (useFirIC) {
                        freeCompilerArgs += "-Xuse-fir-ic"
                    }
                }
                if (renderDiagnosticNames) {
                    freeCompilerArgs += "-Xrender-internal-diagnostic-names"
                }
                if (path !in tasksWithWarnings) {
                    allWarningsAsErrors = !kotlinBuildProperties.disableWerror
                }
                if (project.path in projectsWithEnabledContextReceivers) {
                    freeCompilerArgs += listOf(
                        "-Xcontext-receivers",
                        "-Xdont-poison-binaries-with-prerelease"
                    )
                }
            }
        }
    }
}

// Still compile stdlib, reflect, kotlin.test and scripting runtimes
// with JVM target 1.6 to simplify migration from Kotlin 1.6 to 1.7.
fun Project.applyFixForStdlib16() {
    plugins.withType<KotlinBasePluginWrapper>() {
        dependencies {
            "kotlinCompilerClasspath"(project(":libraries:tools:stdlib-compiler-classpath"))
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
