import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    `jvm-test-suite`
    id("test-symlink-transformation")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val noArgCompilerPlugin = configurations.dependencyScope("noArgCompilerPlugin")
val assignmentCompilerPlugin = configurations.dependencyScope("assignmentCompilerPlugin")
val serializationCompilerPlugin = configurations.dependencyScope("serializationCompilerPlugin")
val serializationCore = configurations.dependencyScope("serializationCore")
val pluginSandbox = configurations.dependencyScope("pluginSandbox")

val noArgCompilerPluginResolvable = configurations.resolvable("noArgCompilerPluginResolvable") {
    extendsFrom(noArgCompilerPlugin.get())
}
val assignmentCompilerPluginResolvable = configurations.resolvable("assignmentCompilerPluginResolvable") {
    extendsFrom(assignmentCompilerPlugin.get())
}
val serializationCompilerPluginResolvable = configurations.resolvable("serializationCompilerPluginResolvable") {
    extendsFrom(serializationCompilerPlugin.get())
}
val serializationCoreResolvable = configurations.resolvable("serializationCoreResolvable") {
    extendsFrom(serializationCore.get())
}
val pluginSandboxResolvable = configurations.resolvable("pluginSandboxResolvable") {
    extendsFrom(pluginSandbox.get())
}

val buildToolsApiImpl = configurations.dependencyScope("buildToolsApiImpl")
val buildToolsApiImplResolvable = configurations.resolvable("buildToolsApiImplResolvable") {
    extendsFrom(buildToolsApiImpl.get())
}

val jsStdlibImpl = configurations.dependencyScope("jsStdlibImpl")
val jsStdlibImplResolvable = configurations.resolvable("jsStdlibImplResolvable") {
    extendsFrom(jsStdlibImpl.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-runtime"))
        attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "js")
    }
}

val wasmStdlibImpl = configurations.dependencyScope("wasmStdlibImpl")
val wasmStdlibImplResolvable = configurations.resolvable("wasmStdlibImplResolvable") {
    extendsFrom(wasmStdlibImpl.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-runtime"))
        attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "wasm")
        attribute(Attribute.of("org.jetbrains.kotlin.wasm.target", String::class.java), "js")
    }
}

val metadataStdlibImpl = configurations.dependencyScope("metadataStdlibImpl")
val metadataStdlibImplResolvable = configurations.resolvable("metadataStdlibImplResolvable") {
    extendsFrom(metadataStdlibImpl.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-runtime"))
        attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "common")
    }
}

val scriptingCompilerPlugin = configurations.dependencyScope("scriptingCompilerPlugin")
val scriptingCompilerPluginResolvable = configurations.resolvable("scriptingCompilerPluginResolvable") {
    extendsFrom(scriptingCompilerPlugin.get())
}

val unpackedResources by configurations.dependencyScope("unpackedResources")
val unpackedResourcesResolvable by configurations.resolvable("unpackedResourcesResolvable") {
    // Wire the dependency declarations
    extendsFrom(unpackedResources)
    // These attributes must be compatible with the producer
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.RESOURCES))
    }
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-compat"))
    api(testFixtures(project(":compiler:test-infrastructure-utils"))) // for `@TestDataPath`/`@TestMetadata`

    api(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    noArgCompilerPlugin(project(":kotlin-noarg-compiler-plugin.embeddable"))
    assignmentCompilerPlugin(project(":kotlin-assignment-compiler-plugin.embeddable"))
    scriptingCompilerPlugin(project(":kotlin-scripting-compiler-embeddable"))
    serializationCompilerPlugin(project(":kotlinx-serialization-compiler-plugin.embeddable"))
    serializationCore(libs.kotlinx.serialization.core)
    pluginSandbox(project(":plugins:plugin-sandbox"))
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-compat"))
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-impl"))
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
    unpackedResources(project(":compiler:build-tools:kotlin-build-tools-api-tests")) {
        isTransitive = false
    }
    jsStdlibImpl(project(":kotlin-stdlib"))
    wasmStdlibImpl(project(":kotlin-stdlib"))
    metadataStdlibImpl(project(":kotlin-stdlib"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
        optIn.add("kotlin.ExperimentalStdlibApi")
        optIn.add("kotlin.io.path.ExperimentalPathApi")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val compatibilityTestsVersions = listOf(
    BuildToolsVersion(KotlinToolingVersion(project.version.toString()), isCurrent = true),
    BuildToolsVersion(KotlinToolingVersion(2, 1, 20, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 2, 21, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 3, 0, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 3, 10, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 3, 20, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 4, 0, null)),
)

val compatibilityTestsExcludedVersions = listOf(
    BuildToolsVersion(KotlinToolingVersion(2, 3, 21, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 2, 20, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 2, 10, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 2, 0, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 1, 21, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 1, 10, null)),
    BuildToolsVersion(KotlinToolingVersion(2, 1, 0, null)),
)

class BuildToolsVersion(val version: KotlinToolingVersion, val isCurrent: Boolean = false) {
    override fun toString() = if (isCurrent) "Snapshot" else version.toString()
}

val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"
val JS_STDLIB_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.jsStdlibClasspath"
val WASM_STDLIB_CLASSSPATH_PROPERTY = "kotlin.build-tools-api.test.wasmStdlibClasspath"
val METADATA_STDLIB_CLASSSPATH_PROPERTY = "kotlin.build-tools-api.test.metadataStdlibClasspath"

fun Test.ensureExecutedAgainstExpectedBuildToolsImplVersion(version: BuildToolsVersion) {
    if (version.isCurrent) return
    val compilerClasspathProperty =
        COMPILER_CLASSPATH_PROPERTY // to make the task action configuration cache-friendly, we have to copy it to a local var
    // the check is required for the case when Gradle substitutes external dependencies with project ones
    doFirst {
        // we cannot check systemProperties because the classpath is configured in addClasspathProperty via jvmArgumentProviders
        val compilerClasspath = jvmArgumentProviders
            .map { it.asArguments().joinToString("|||") }
            .find { compilerClasspathProperty in it }
            ?.substring("-D$compilerClasspathProperty=".length)
            ?.substringBefore("|||")
            ?.split(File.pathSeparator)
            ?: error("Failed to parse compiler classpath system property $compilerClasspathProperty")
        check(
            compilerClasspath.any { "kotlin-build-tools-impl-${version}" in it }) {
            "runtime classpath must contain kotlin-build-tools-impl:$version"
        }
    }
}

fun SourceSet.configureCompatibilitySourceDirectories(testSuiteName: String) {
    java.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/$testSuiteName/java"),
        )
    )
    kotlin.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/$testSuiteName/java"),
            layout.projectDirectory.dir("src/$testSuiteName/kotlin"),
        )
    )
    resources.setSrcDirs(
        listOf(
            layout.projectDirectory.dir("src/$testSuiteName/resources"),
        )
    )
}

// just add a new test suit name here and that's it
val businessLogicTestSuits = setOf(
    "testExample",
    "testEscapableCharacters",
    "testInputChangesTracking",
    "testCrossModuleIncrementalChanges",
    "testFirRunner",
    "testCriToolchain",
    "testCompilerPlugins",
    "testBuildMetrics",
    "testKotlinLogger",
    "testDefaultOptions",
    "testDaemonOptions",
    "testInternalInputsTracker",
    "testAbiValidation",
    "testRestrictedArguments",
    "testClasspathMetadata",
)

fun JvmTestSuite.addStdLibClasspaths() {
    targets.all {
        testTask.configure {
            addClasspathProperty(jsStdlibImplResolvable.get(), JS_STDLIB_CLASSPATH_PROPERTY)
            addClasspathProperty(wasmStdlibImplResolvable.get(), WASM_STDLIB_CLASSSPATH_PROPERTY)
            addClasspathProperty(metadataStdlibImplResolvable.get(), METADATA_STDLIB_CLASSSPATH_PROPERTY)
        }
    }
}

fun JvmTestSuite.addSnapshotBuildToolsImpl() {
    targets.all {
        testTask.configure {
            addClasspathProperty(buildToolsApiImplResolvable.get(), COMPILER_CLASSPATH_PROPERTY)
        }
    }
    addStdLibClasspaths()
}

fun JvmTestSuite.addSpecificBuildToolsImpl(version: String) {
    val baseName = "buildToolsApiImpl$version"
    val resolvableSuffix = "Resolvable"
    val configurationsExist = baseName in configurations.names
    val resolvableConfiguration = if (configurationsExist) {
        configurations.named("$baseName$resolvableSuffix")
    } else {
        val buildToolsApiImpl = configurations.dependencyScope(baseName)
        val buildToolsApiImplResolvable = configurations.resolvable("$baseName$resolvableSuffix") {
            extendsFrom(buildToolsApiImpl.get())
        }
        project.dependencies {
            buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-api"))
            buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-compat"))
            buildToolsApiImpl("org.jetbrains.kotlin:kotlin-build-tools-impl:${version}")
        }
        buildToolsApiImplResolvable
    }

    targets.all {
        testTask.configure {
            addClasspathProperty(resolvableConfiguration.get(), COMPILER_CLASSPATH_PROPERTY)
        }
    }
    addStdLibClasspaths()
}

testing {
    suites {
        for (suit in businessLogicTestSuits) {
            register<JvmTestSuite>(suit)
        }

        var configuredIdeaSourceSets = false
        for (implVersion in compatibilityTestsVersions) {
            register<JvmTestSuite>("testCompatibility${implVersion}") {
                if (!kotlinBuildProperties.isInIdeaSync.get() || !configuredIdeaSourceSets) {
                    sources.configureCompatibilitySourceDirectories("testCompatibility")
                }
                if (implVersion.isCurrent) {
                    addSnapshotBuildToolsImpl()
                } else {
                    addSpecificBuildToolsImpl(implVersion.toString())
                }
                targets.all {
                    projectTests {
                        testTask(
                            taskName = testTask.name,
                            javaLauncher = JdkMajorVersion.JDK_1_8,
                            skipInLocalBuild = false
                        ) {
                            @OptIn(TemporaryTestFederationApi::class)
                            smokeTestConfig = SmokeTestConfig.RunAllTests

                            ensureExecutedAgainstExpectedBuildToolsImplVersion(implVersion)
                            systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        }
                    }
                }
            }
            configuredIdeaSourceSets = true
        }

        withType<JvmTestSuite>().configureEach configureSuit@{
            val isRegular = this@configureSuit.name in businessLogicTestSuits
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())

                implementation(project())
                implementation(project(":kotlin-tooling-core"))
                implementation(project(":compiler:test-security-manager"))
                implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
                implementation(project(":compiler:arguments"))
                if (isRegular) {
                    addSnapshotBuildToolsImpl()
                }
            }

            targets.all {
                if (businessLogicTestSuits.any { testTask.name.startsWith(it) }) {
                    projectTests {
                        testTask(
                            taskName = testTask.name,
                            javaLauncher = JdkMajorVersion.JDK_1_8,
                            skipInLocalBuild = false
                        ) {
                            systemProperty("kotlin.build-tools-api.log.level", "DEBUG")

                        }
                    }
                }
                testTask.configure {
                    systemProperty(
                        "kotlin.daemon.custom.run.files.path.for.tests",
                        "build/daemon"
                    )
                    addClasspathProperty(unpackedResourcesResolvable, "kotlin.test.templates.classpath")
                }
            }
        }

        named<JvmTestSuite>("testDaemonOptions") {
            dependencies {
                implementation(project(":daemon-common"))
            }
        }

        named<JvmTestSuite>("testEscapableCharacters") {
            configurations.named(sources.runtimeClasspathConfigurationName) {
                testSymlinkTransformation.resolveAgainstSymlinkedArtifacts(this)
            }
        }

        named<JvmTestSuite>("testDefaultOptions") testSuite@{
            dependencies {
                implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
                implementation(project(":daemon-common"))
                implementation(project(":compiler:build-tools:kotlin-build-tools-impl"))
            }
        }

        named<JvmTestSuite>("testInputChangesTracking") {
            dependencies {
                implementation(project(":compiler:build-tools:kotlin-build-statistics"))
            }
        }

        named<JvmTestSuite>("testInternalInputsTracker") {
            dependencies {
                implementation(project(":compiler:build-tools:kotlin-build-tools-impl"))
            }
        }

        named<JvmTestSuite>("testRestrictedArguments") {
            dependencies {
                implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
            }
        }

        named<JvmTestSuite>("testCompilerPlugins") {
            dependencies {
                compileOnly(project(":kotlin-scripting-common"))
                implementation(project(":compiler:build-tools:kotlin-build-statistics"))
            }
            targets.all {
                testTask.configure {
                    addClasspathProperty(noArgCompilerPluginResolvable.get(), "NOARG_COMPILER_PLUGIN")
                    addClasspathProperty(assignmentCompilerPluginResolvable.get(), "ASSIGNMENT_COMPILER_PLUGIN")
                    addClasspathProperty(scriptingCompilerPluginResolvable.get(), "SCRIPTING_COMPILER_PLUGIN")
                    addClasspathProperty(serializationCompilerPluginResolvable.get(), "SERIALIZATION_COMPILER_PLUGIN")
                    addClasspathProperty(serializationCoreResolvable.get(), "SERIALIZATION_CORE")
                    addClasspathProperty(pluginSandboxResolvable.get(), "PLUGIN_SANDBOX")

                    // those classes use compileOnly dependency on scripting and should not be considered as containing test classes to avoid runtime failures
                    exclude(
                        "org/jetbrains/kotlin/buildtools/tests/compilation/GreetScriptTemplate.class",
                        "org/jetbrains/kotlin/buildtools/tests/compilation/GreetScriptCustomExtensionTemplate.class",
                        "org/jetbrains/kotlin/buildtools/tests/compilation/GreetScriptMyExtensionTemplate.class",
                        "org/jetbrains/kotlin/buildtools/tests/compilation/GreetScriptDefinition.class",
                    )
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.matching { it.name != "testExample" }) // do not run example tests by default
}

val checkCompatibilityCoverageTask = checkCompatibilityCoverage(
    btaVersion = project.version.toString(),
    compatibilityTestsVersions = compatibilityTestsVersions.map { it.version },
    compatibilityTestsExcludedVersions = compatibilityTestsExcludedVersions.map { it.version },
)

fun Project.checkCompatibilityCoverage(
    btaVersion: String,
    compatibilityTestsVersions: List<KotlinToolingVersion>,
    compatibilityTestsExcludedVersions: List<KotlinToolingVersion>,
): TaskProvider<JavaExec> {
    val checkerClasspath = configurations.register("checkerClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    dependencies {
        checkerClasspath(project(":compiler:build-tools:kotlin-build-tools-version-coverage-check"))
    }

    return tasks.register<JavaExec>("checkCompatibilityCoverage") {
        group = "verification"
        description = "Verify that tests cover all versions required by backward compatibility guarantees"
        classpath = checkerClasspath.get()
        mainClass.set("org.jetbrains.kotlin.buildtools.versioncoverage.MainKt")
        args(
            btaVersion,
            "backward",
            compatibilityTestsVersions.joinToString(",") { it.toString() },
            compatibilityTestsExcludedVersions.joinToString(",") { it.toString() },
        )
    }
}
