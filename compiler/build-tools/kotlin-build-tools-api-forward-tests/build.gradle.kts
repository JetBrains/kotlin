import org.gradle.kotlin.dsl.invoke
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
val noArgCompilerPluginResolvable = configurations.resolvable("noArgCompilerPluginResolvable") {
    extendsFrom(noArgCompilerPlugin.get())
}

val assignmentCompilerPlugin = configurations.dependencyScope("assignmentCompilerPlugin")
val assignmentCompilerPluginResolvable = configurations.resolvable("assignmentCompilerPluginResolvable") {
    extendsFrom(assignmentCompilerPlugin.get())
}

val serializationCompilerPlugin = configurations.dependencyScope("serializationCompilerPlugin")
val serializationCompilerPluginResolvable = configurations.resolvable("serializationCompilerPluginResolvable") {
    extendsFrom(serializationCompilerPlugin.get())
}

val serializationCore = configurations.dependencyScope("serializationCore")
val serializationCoreResolvable = configurations.resolvable("serializationCoreResolvable") {
    extendsFrom(serializationCore.get())
}

val pluginSandbox = configurations.dependencyScope("pluginSandbox")
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

val unpackedResources = configurations.dependencyScope("unpackedResources")
val unpackedResourcesResolvable = configurations.resolvable("unpackedResourcesResolvable") {
    // Wire the dependency declarations
    extendsFrom(unpackedResources)
    // These attributes must be compatible with the producer
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.RESOURCES))
    }
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:build-tools:kotlin-build-tools-api-forward-tests:shared"))
    compileOnly(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`
    api(testFixtures(project(":compiler:test-infrastructure-utils"))) // for `@TestDataPath`/`@TestMetadata`
    api(platform(libs.junit.bom))
    compileOnly(libs.junit.jupiter.engine)
    compileOnly(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-impl"))
    buildToolsApiImpl(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
    unpackedResources(project(":compiler:build-tools:kotlin-build-tools-api-tests")) {
        isTransitive = false
    }
    jsStdlibImpl(project(":kotlin-stdlib"))
    wasmStdlibImpl(project(":kotlin-stdlib"))
    metadataStdlibImpl(project(":kotlin-stdlib"))
    noArgCompilerPlugin(project(":kotlin-noarg-compiler-plugin.embeddable"))
    assignmentCompilerPlugin(project(":kotlin-assignment-compiler-plugin.embeddable"))
    scriptingCompilerPlugin(project(":kotlin-scripting-compiler-embeddable"))
    serializationCompilerPlugin(project(":kotlinx-serialization-compiler-plugin.embeddable"))
    serializationCore(libs.kotlinx.serialization.core)
    pluginSandbox(project(":plugins:plugin-sandbox"))
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
    KotlinToolingVersion(2, 4, 0, null),
    KotlinToolingVersion(2, 4, 20, "Beta1"),
)

val KotlinToolingVersion.sourceSetName get() = "shared" + this.toString().replace(".", "_").replace("-", "_")

val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"
val JS_STDLIB_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.jsStdlibClasspath"
val WASM_STDLIB_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.wasmStdlibClasspath"
val METADATA_STDLIB_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.metadataStdlibClasspath"

fun JvmTestSuite.ensureExecutedAgainstExpectedBuildToolsApiVersion(version: KotlinToolingVersion) {
    targets.all {
        projectTests {
            testTask.configure {
                // the check is required for the case when Gradle substitutes external dependencies with project ones
                doFirst {
                    check(
                        classpath.files.any { "kotlin-build-tools-api-${version}.jar" in it.name }
                    ) {
                        "runtime classpath must contain kotlin-build-tools-api-$version.jar but was: ${classpath.joinToString()}"
                    }
                }
            }
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
    "testCompatibility",
)

fun JvmTestSuite.addSnapshotBuildToolsImpl() {
    targets.all {
        testTask.configure {
            addClasspathProperty(buildToolsApiImplResolvable.get(), COMPILER_CLASSPATH_PROPERTY)
            addClasspathProperty(jsStdlibImplResolvable.get(), JS_STDLIB_CLASSPATH_PROPERTY)
            addClasspathProperty(wasmStdlibImplResolvable.get(), WASM_STDLIB_CLASSPATH_PROPERTY)
            addClasspathProperty(metadataStdlibImplResolvable.get(), METADATA_STDLIB_CLASSPATH_PROPERTY)
            addClasspathProperty(unpackedResourcesResolvable, "kotlin.test.templates.classpath")
        }
    }
}

fun JvmTestSuite.addSpecificBuildToolsApi(version: KotlinToolingVersion) {
    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-build-tools-api:${version}")
        implementation(sourceSets.getByName(version.sourceSetName).output)
    }
}


for (apiVersion in compatibilityTestsVersions) {
    sourceSets {
        apiVersion.sourceSetName {
            kotlin.srcDir("src/shared$apiVersion/kotlin")
            resources.srcDir("src/shared$apiVersion/resources")
        }
    }
    dependencies {
        "${apiVersion.sourceSetName}CompileOnly"(project())
        "${apiVersion.sourceSetName}CompileOnly"("org.jetbrains.kotlin:kotlin-build-tools-api:${apiVersion}")
        "${apiVersion.sourceSetName}CompileOnly"(project(":kotlin-tooling-core"))
        "${apiVersion.sourceSetName}CompileOnly"(libs.junit.jupiter.engine)
        "${apiVersion.sourceSetName}CompileOnly"(libs.junit.jupiter.params)
    }
}

testing {
    suites {
        for (apiVersion in compatibilityTestsVersions) {
            register<JvmTestSuite>("testForwardCompatibility${apiVersion}") {
                addSnapshotBuildToolsImpl()
                addSpecificBuildToolsApi(apiVersion)
                sources.configureCompatibilitySourceDirectories("testForwardCompatibility$apiVersion")
                ensureExecutedAgainstExpectedBuildToolsApiVersion(apiVersion)
            }
            for (suit in businessLogicTestSuits) {
                register<JvmTestSuite>(suit + apiVersion) {
                    addSnapshotBuildToolsImpl()
                    addSpecificBuildToolsApi(apiVersion)
                    sources.configureCompatibilitySourceDirectories(suit)
                    ensureExecutedAgainstExpectedBuildToolsApiVersion(apiVersion)
                }
            }
            named<JvmTestSuite>("testDaemonOptions$apiVersion") {
                dependencies {
                    compileOnly(project(":daemon-common"))
                }
            }

            named<JvmTestSuite>("testEscapableCharacters$apiVersion") {
                configurations.named(sources.runtimeClasspathConfigurationName) {
                    testSymlinkTransformation.resolveAgainstSymlinkedArtifacts(this)
                }
            }

            named<JvmTestSuite>("testDefaultOptions$apiVersion") testSuite@{
                dependencies {
                    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
                    implementation(project(":daemon-common")) { isTransitive = false }
                }
            }

            named<JvmTestSuite>("testInputChangesTracking$apiVersion") {
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-build-statistics:${apiVersion}")
                }
            }

            named<JvmTestSuite>("testInternalInputsTracker$apiVersion") {
                dependencies {
                    implementation(project(":compiler:build-tools:kotlin-build-tools-impl")) { isTransitive = false }
                }
            }

            named<JvmTestSuite>("testRestrictedArguments$apiVersion") {
                dependencies {
                    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
                }
            }

            named<JvmTestSuite>("testCompilerPlugins$apiVersion") {
                dependencies {
                    compileOnly(project(":kotlin-scripting-common"))
                    implementation("org.jetbrains.kotlin:kotlin-build-statistics:${apiVersion}")
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
                            "org/jetbrains/kotlin/buildtools/forward/tests/compilation/GreetScriptTemplate.class",
                            "org/jetbrains/kotlin/buildtools/forward/tests/compilation/GreetScriptCustomExtensionTemplate.class",
                            "org/jetbrains/kotlin/buildtools/forward/tests/compilation/GreetScriptMyExtensionTemplate.class",
                            "org/jetbrains/kotlin/buildtools/forward/tests/compilation/GreetScriptDefinition.class",
                        )
                    }
                }
            }
        }

        withType<JvmTestSuite>().configureEach configureSuit@{
            dependencies {
                useJUnitJupiter(libs.versions.junit5.get())
                runtimeOnly(libs.junit.platform.launcher)

                implementation(project())
                implementation(project(":compiler:build-tools:kotlin-build-tools-api-forward-tests:shared"))
                implementation(project(":kotlin-tooling-core"))
                implementation(project(":compiler:test-security-manager"))
                implementation(project(":compiler:arguments"))
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

                        systemProperty("kotlin.build-tools-api.log.level", "DEBUG")
                        systemProperty(
                            "kotlin.daemon.custom.run.files.path.for.tests",
                            "build/daemon"
                        )
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites)
}
