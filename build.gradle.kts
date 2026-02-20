import org.gradle.crypto.checksum.Checksum
import org.gradle.plugins.ide.idea.model.IdeaModel

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion.get()}")
    }

    /**
     * Global Security Fixes for Common Dependencies
     *
     * Enforces minimum secure versions for commonly used libraries across all subprojects.
     * These overrides address known vulnerabilities in transitive dependencies that might
     * be pulled in by various subprojects.
     *
     * Affected Libraries:
     * └── org.apache.commons
     *     ├── commons-compress:* → 1.27.1
     *     ├── commons-io:* → 2.18.0
     *     └── commons-lang3:* → 3.18.0
     *
     * Mitigated Vulnerabilities:
     * 1. Commons Compress
     *    - CVE-2024-26308: Potential security vulnerability
     *    - CVE-2024-25710: Input validation weakness
     *    - CVE-2023-42503: Potential code execution risk
     *
     * 2. Commons IO
     *    - CVE-2024-26308: Security vulnerability
     *    - CVE-2023-42503: Input processing risk
     *
     * 3. Commons Lang
     *    - CVE-2025-48924: Uncontrolled Recursion vulnerability
     */
    configurations.all {
        resolutionStrategy.eachDependency {
            // Apache Commons libraries
            if (requested.group == "org.apache.commons" && requested.name == "commons-compress") {
                useVersion(libs.versions.commons.compress.get())
                because("CVE-2024-26308, CVE-2024-25710, CVE-2023-42503")
            }
            if (requested.group == "commons-io" && requested.name == "commons-io") {
                useVersion(libs.versions.commons.io.get())
                because("CVE-2024-26308, CVE-2023-42503")
            }
            if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
                useVersion(libs.versions.commons.lang.get())
                because("CVE-2025-48924")
            }
        }
    }
}

plugins {
    id("root-config")
    base
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.0.1" // this version should be in sync with repo/buildsrc-compat/build.gradle.kts
    id("build-time-report")
    id("java-instrumentation")
    id("modularized-test-configurations")
    id("resolve-dependencies")
    id("org.gradle.crypto.checksum") version "1.4.0"
    alias(libs.plugins.kotlinx.bcv) apply false
    signing
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        id("kotlin.native.build-tools-conventions") apply false
    }
    `jvm-toolchains`
    alias(libs.plugins.gradle.node) apply false
    id("nodejs-cache-redirector-configuration")
    id("gradle-plugins-documentation") apply false
    id("com.autonomousapps.dependency-analysis") version "3.4.0"
    id("project-tests-convention") apply false
    id("test-data-manager-root")
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

val defaultSnapshotVersion: String by extra
findProperty("deployVersion")?.let {
    assert(findProperty("build.number") != null) { "`build.number` parameter is expected to be explicitly set with the `deployVersion`" }
}
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(
    findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
        if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else defaultSnapshotVersion
    } ?: buildNumber
)

val kotlinLanguageVersion: String by extra
val kotlinApiVersionForModulesUsedInIDE: String by extra

extra["kotlin_root"] = rootDir

val jpsBootstrap by configurations.creating

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val artifactsDir = "$distDir/artifacts"
val ideaPluginDir = "$artifactsDir/ideaPlugin/Kotlin"

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)

rootProject.apply {
    from(rootProject.file("gradle/versions.gradle.kts"))
    from(rootProject.file("gradle/checkArtifacts.gradle.kts"))
    from(rootProject.file("gradle/checkCacheability.gradle.kts"))
    from(rootProject.file("gradle/retryPublishing.gradle.kts"))
}

IdeVersionConfigurator.setCurrentIde(project)

if (!project.hasProperty("versions.kotlin-native")) {
    /**
     * "versions.kotlin-native" is the version of K/N dist that will be baked into KGP and that KGP will try to resolve to run K/N
     * compilations (including in KGP tests).
     */
    extra["versions.kotlin-native"] = if (kotlinBuildProperties.alignKotlinNativeVersionInTCBuilds) {
        kotlinVersion
    } else if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        kotlinBuildProperties.defaultSnapshotVersion.get()
    } else {
        "2.4.0-dev-3656"
    }
}

val irCompilerModules = ProjectModuleLists.irCompilerModules.also { extra["irCompilerModules"] = it }

val irCompilerModulesForIDE = ProjectModuleLists.irCompilerModulesForIDE.also { extra["irCompilerModulesForIDE"] = it }

val commonCompilerModules = ProjectModuleLists.commonCompilerModules.also { extra["commonCompilerModules"] = it }

val firCompilerCoreModules = ProjectModuleLists.firCompilerCoreModules.also { extra["firCompilerCoreModules"] = it }

val firAllCompilerModules = ProjectModuleLists.firAllCompilerModules.also { extra["firAllCompilerModules"] = it }

val fe10CompilerModules = ProjectModuleLists.fe10CompilerModules.also { extra["fe10CompilerModules"] = it }

extra["compilerModules"] = ProjectModuleLists.compilerModules

/**
 * An array of projects used in the IntelliJ Kotlin Plugin.
 *
 * Experimental declarations from Kotlin stdlib cannot be used in those projects to avoid stdlib binary compatibility problems.
 * See KT-62510 for details.
 */
val projectsUsedInIntelliJKotlinPlugin = ProjectModuleLists.projectsUsedInIntelliJKotlinPlugin.also { extra["projectsUsedInIntelliJKotlinPlugin"] = it }

extra["kotlinJpsPluginEmbeddedDependencies"] = ProjectModuleLists.kotlinJpsPluginEmbeddedDependencies
extra["kotlinJpsPluginMavenDependencies"] = ProjectModuleLists.kotlinJpsPluginMavenDependencies
extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"] = listOf(
    commonDependency("org.jetbrains.kotlin:kotlin-reflect")
)

extra["compilerArtifactsForIde"] = listOfNotNull(
    ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:compose-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:js-plain-objects-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:scripting-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide",
    ":prepare:ide-plugin-dependencies:js-ir-runtime-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-cli-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-gradle-statistics-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-classpath",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-testdata-for-ide",
    ":prepare:ide-plugin-dependencies:kotlinx-serialization-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:noarg-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:sam-with-receiver-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:assignment-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:parcelize-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:lombok-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-dataframe-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-objcexport-header-generator-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-swift-export-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-testdata-for-ide",
    ":prepare:ide-plugin-dependencies:low-level-api-fir-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-impl-base-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-impl-base-tests-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-k2-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-k2-tests-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-fe10-tests-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-platform-interface-for-ide",
    ":prepare:ide-plugin-dependencies:symbol-light-classes-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-standalone-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-ir-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fir-for-ide",
    ":prepare:kotlin-jps-plugin",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-dependencies",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":plugins:parcelize:parcelize-runtime",
    ":plugins:jvm-abi-gen",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-test",
    ":kotlin-daemon",
    ":kotlin-compiler",
    ":kotlin-annotations-jvm",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-reflect",
    ":kotlin-main-kts",
    ":kotlin-dom-api-compat",
    ":compiler:build-tools:kotlin-build-tools-api",
    ":compiler:build-tools:kotlin-build-tools-impl",
    ":compiler:build-tools:kotlin-build-tools-cri-impl",
)

val coreLibProjects by extra {
    listOfNotNull(
        ":kotlin-stdlib",
        ":kotlin-stdlib-jdk7",
        ":kotlin-stdlib-jdk8",
        ":kotlin-test",
        ":kotlin-reflect",
        ":kotlin-metadata-jvm",
    )
}
val mppProjects by extra { ProjectModuleLists.mppProjects }

val projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib by extra { ProjectModuleLists.projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib }

val gradlePluginProjects = listOf(
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin-api",
    ":kotlin-gradle-plugin-annotations",
    ":kotlin-gradle-plugin-idea",
    ":kotlin-gradle-plugin-idea-proto",
    ":kotlin-gradle-plugin-tcs-android",
    ":compose-compiler-gradle-plugin",
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-power-assert",
    ":kotlin-sam-with-receiver",
    ":kotlin-parcelize-compiler",
    ":kotlin-lombok",
    ":kotlin-assignment",
    ":kotlin-dataframe"
)

val ignoreTestFailures by extra(project.kotlinBuildProperties.ignoreTestFailures)



gradle.taskGraph.whenReady {
    fun Boolean.toOnOff(): String = if (this) "on" else "off"
    val profile = if (isTeamcityBuild.get()) "CI" else "Local"

    val proguardMessage = "proguard is ${kotlinBuildProperties.proguard.toOnOff()}"
    val jarCompressionMessage = "jar compression is ${kotlinBuildProperties.jarCompression.toOnOff()}"

    logger.warn(
        "$profile build profile is active ($proguardMessage, $jarCompressionMessage). " +
                "Use -Pteamcity=<true|false> to reproduce CI/local build"
    )

    allTasks.filterIsInstance<org.gradle.jvm.tasks.Jar>().forEach { task ->
        task.entryCompression = if (kotlinBuildProperties.jarCompression)
            ZipEntryCompression.DEFLATED
        else
            ZipEntryCompression.STORED
    }
}

val dist = tasks.register("dist") {
    dependsOn(":kotlin-compiler:dist")
}

tasks.register("createIdeaHomeForTests") {
    val ideaBuildNumberFileForTests = ideaBuildNumberFileForTests()
    val intellijSdkVersion = rootProject.extra["versions.intellijSdk"]
    outputs.dir(ideaHomePathForTests())
    doFirst {
        with(ideaBuildNumberFileForTests.get().asFile) {
            parentFile.mkdirs()
            writeText("IC-$intellijSdkVersion")
        }
    }
}

tasks {
    // compileAll is registered per-project by common-configuration plugin.
    // Cross-project wiring is done in settings.gradle gradle.projectsEvaluated block.

    named<Delete>("clean") {
        delete(distDir)
        delete(layout.buildDirectory.dir("repo"))
    }

    register<Delete>("cleanupArtifacts") {
        delete = setOf(artifactsDir)
    }

    fun aggregateLibsTask(name: String, projectTask: String, projects: List<String>) =
        register(name) {
            projects.forEach { dependsOn("$it:$projectTask") }
        }

    val coreLibsPublishable = coreLibProjects + listOf(":kotlin-stdlib-common")
    val coreLibsBuildable = coreLibProjects + listOf(":kotlin-stdlib-jvm-minimal-for-test", ":kotlin-stdlib-js-ir-minimal-for-test")

    aggregateLibsTask(
        "coreLibsClean", "clean",
        (coreLibProjects + coreLibsBuildable + coreLibsPublishable).distinct() +
                ":kotlin-stdlib:samples"
    )

    aggregateLibsTask("coreLibsAssemble", "assemble", coreLibsBuildable)
    aggregateLibsTask("coreLibsInstall", "install", coreLibsPublishable)
    aggregateLibsTask("coreLibsPublish", "publish", coreLibsPublishable)
    aggregateLibsTask(
        "coreLibsTest", "check",
        coreLibsBuildable + listOfNotNull(
            ":kotlin-stdlib:samples",
            ":kotlin-test:kotlin-test-js-it",
            ":tools:binary-compatibility-validator",
            ":tools:jdk-api-validator",
        )
    )

    register("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn("$it:check")
        }
    }

    register("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    register("jvmCompilerTest") {
        dependsOn(
            ":compiler:tests-common-new:test",
            ":compiler:container:test",
            ":compiler:tests-java8:test",
            ":compiler:tests-spec:test",
            ":compiler:tests-against-klib:test"
        )
    }

    register("testsForBootstrapBuildTest") {
        dependsOn(":compiler:tests-common-new:test")
    }

    register("jvmCompilerIntegrationTest") {
        dependsOn(
            ":kotlin-compiler-embeddable:test",
            ":kotlin-compiler-client-embeddable:test"
        )
    }

    register("jsCompilerTest") {
        dependsOn(":js:js.tests:jsTest")
    }

    register("wasmCompilerTest") {
        // KTI-2670: TODO: don't invoke this obsolete task in KTI
    }

    register("wasmFirCompilerTest") {
        dependsOn(":wasm:wasm.tests:test")
        // Windows WABT release requires Visual C++ Redistributable
        if (!kotlinBuildProperties.isTeamcityBuild.get() || !org.gradle.internal.os.OperatingSystem.current().isWindows) {
            dependsOn(":wasm:wasm.ir:test")
        }
    }

    // These tests run Native compiler and will be run in many different compilation modes that the compiler supports:
    // - different optimization modes
    // - different cache policies
    // - different GCs
    // ...
    register("nativeCompilerTest") {
        dependsOn(":kotlin-atomicfu-compiler-plugin:nativeTest")
        dependsOn(":plugins:plugin-sandbox:nativeTest")
        dependsOn(":libraries:tools:analysis-api-based-klib-reader:check")
        dependsOn(":native:native.tests:test")
        dependsOn(":native:native.tests:cli-tests:check")
        dependsOn(":native:native.tests:codegen-box:check")
        dependsOn(":native:native.tests:driver:check")
        dependsOn(":native:native.tests:gc-fuzzing-tests:engine:check")
        dependsOn(":native:native.tests:stress:check")
        dependsOn(":native:native.tests:klib-compatibility:check")
        dependsOn(":native:native.tests:litmus-tests:check")
    }

    // Similar to nativeCompilerTest, but should be executed only on macOS host as these tests
    // technically or semantically depend on Xcode SDK.
    register("nativeAppleSpecificTests") {
        dependsOn(":native:objcexport-header-generator:check")
        dependsOn(":native:swift:swift-export-embeddable:testCoroutinesITWithEmbeddable")
        dependsOn(":native:swift:swift-export-embeddable:testExternalITWithEmbeddable")
        dependsOn(":native:swift:swift-export-embeddable:testSimpleITWithEmbeddable")
        dependsOn(":native:swift:swift-export-standalone:check")
        dependsOn(":native:swift:swift-export-ide:test")
        dependsOn(":native:swift:sir-light-classes:check")
    }

    // These are unit tests of Native compiler
    register("nativeCompilerUnitTest") {
        dependsOn(":native:kotlin-native-utils:check")
        if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
            dependsOn(":kotlin-native:Interop:Indexer:check")
            dependsOn(":kotlin-native:Interop:StubGenerator:check")
            dependsOn(":kotlin-native:backend.native:check")
            dependsOn(":kotlin-native:tools:kdumputil:check")
            dependsOn(":kotlin-native:common:env:check")
            dependsOn(":kotlin-native:common:files:check")
            dependsOn(":kotlin-native:libclangInterop:check")
            dependsOn(":kotlin-native:llvmInterop:check")
        }
    }

    register("klibIrTest") {
        dependsOn(":tools:binary-compatibility-validator:check")
        dependsOn(":native:native.tests:klib-ir-inliner:check")
    }

    register("firCompilerTest") {
        dependsOn(":compiler:fir:raw-fir:psi2fir:test")
        dependsOn(":compiler:fir:raw-fir:light-tree2fir:test")
        dependsOn(":compiler:fir:analysis-tests:test")
        dependsOn(":compiler:fir:analysis-tests:legacy-fir-tests:test")
        dependsOn(":compiler:fir:fir2ir:aggregateTests")
    }

    register("nightlyFirCompilerTest") {
        dependsOn(":compiler:fir:fir2ir:nightlyTests")
        dependsOn(":compiler:fastJarFSLongTests")
    }

    register("scriptingJvmTest") {
        dependsOn("dist")
        dependsOn(":kotlin-scripting-compiler:test")
        dependsOn(":kotlin-scripting-common:test")
        dependsOn(":kotlin-scripting-jvm:test")
        dependsOn(":kotlin-scripting-jvm-host-test:test")
        dependsOn(":plugins:scripting:scripting-tests:test")
        dependsOn(":kotlin-scripting-dependencies:test")
        dependsOn(":kotlin-scripting-dependencies-maven:test")
        dependsOn(":kotlin-scripting-dependencies-maven-all:test")
        // see comments on the task in kotlin-scripting-jvm-host-test
//        dependsOn(":kotlin-scripting-jvm-host-test:embeddableTest")
        dependsOn(":kotlin-main-kts-test:test")
    }

    register("scriptingK1JvmTest") {
        dependsOn(":kotlin-scripting-compiler:testWithK1")
        dependsOn(":kotlin-scripting-jvm-host-test:testWithK1")
        dependsOn(":kotlin-main-kts-test:testWithK1")
        dependsOn(":kotlin-scripting-jsr223-test:test")
        dependsOn(":kotlin-scripting-jsr223-test:embeddableTest")
        dependsOn(":kotlin-scripting-ide-services-test:test")
        dependsOn(":kotlin-scripting-ide-services-test:embeddableTest")
    }

    register("scriptingTest") {
        dependsOn("scriptingJvmTest")
        dependsOn("scriptingK1JvmTest")
    }

    register("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("miscCompilerTest")
    }

    register("miscCompilerTest") {
        dependsOn(":compiler:test")
        dependsOn(":compiler:tests-integration:test")
        dependsOn(":kotlin-compiler-embeddable:test")
        dependsOn("incrementalCompilationTest")
        dependsOn("scriptingTest")
        dependsOn("jvmCompilerIntegrationTest")
        dependsOn("compilerPluginTest")
        dependsOn(":kotlin-daemon-tests:test")
        dependsOn(":compiler:arguments:test")
        dependsOn(":compiler:multiplatform-parsing:jvmTest")
        dependsOn(":compiler:fir:modularized-tests:test")
    }

    register("miscTest") {
        dependsOn("coreLibsTest")
        dependsOn("toolsTest")
        dependsOn("examplesTest")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":kotlin-build-common:testJUnit5")
        dependsOn(":core:descriptors.runtime:test")
        dependsOn(":kotlin-util-io:test")
        dependsOn(":kotlin-util-klib:test")
        dependsOn(":kotlin-util-klib-abi:test")
        dependsOn(":kotlinx-metadata-klib:test")
        dependsOn(":compiler:ir.validation:test")
        dependsOn(":generators:test")
        dependsOn(":kotlin-gradle-plugin-dsl-codegen:test")
    }

    register("incrementalCompilationTest") {
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":compiler:incremental-compilation-impl:testJvmICWithJdk11")
    }

    register("compilerPluginTest") {
        dependsOn(":kotlin-allopen-compiler-plugin:test")
        dependsOn(":kotlin-assignment-compiler-plugin:test")
        dependsOn(":kotlin-atomicfu-compiler-plugin:test")
        dependsOn(":plugins:plugin-sandbox:test")
        dependsOn(":plugins:plugin-sandbox:plugin-sandbox-ic-test:test")
        dependsOn(":plugins:jvm-abi-gen:test")
        dependsOn(":plugins:js-plain-objects:compiler-plugin:test")
        dependsOn(":kotlinx-serialization-compiler-plugin:test")
        dependsOn(":kotlin-lombok-compiler-plugin:test")
        dependsOn(":kotlin-noarg-compiler-plugin:test")
        dependsOn(":kotlin-sam-with-receiver-compiler-plugin:test")
        dependsOn(":kotlin-power-assert-compiler-plugin:test")
        dependsOn(":plugins:plugins-interactions-testing:test")
        dependsOn(":kotlin-dataframe-compiler-plugin:test")
    }

    register("toolsTest") {
        dependsOn(":tools:kotlinp-jvm:test")
        dependsOn(":native:kotlin-klib-commonizer:test")
//        dependsOn(":native:kotlin-klib-commonizer-api:test")
        dependsOn(":kotlin-tooling-core:check")
        dependsOn(":kotlin-tooling-metadata:check")
        dependsOn(":compiler:build-tools:kotlin-build-tools-api:check")
        dependsOn(":compiler:build-tools:kotlin-build-tools-api-tests:check")
        dependsOn(":tools:ide-plugin-dependencies-validator:test")
        dependsOn(":tools:stats-analyser:test")
        dependsOn(":libraries:tools:abi-validation:abi-tools:check")
        dependsOn(":libraries:tools:abi-validation:abi-tools-api:check")
        dependsOn(":libraries:tools:abi-validation:abi-tools-embeddable:check")
        dependsOn(":libraries:tools:abi-validation:abi-tools-tests:check")
    }

    register("examplesTest") {
        dependsOn("dist")
        dependsOn(":examples:annotation-processor-example:check")
        dependsOn(":examples:scripting-jvm-simple-script:check")
        dependsOn(":examples:scripting-jvm-simple-script-host:check")
        dependsOn(":examples:scripting-jvm-maven-deps:check")
        dependsOn(":examples:scripting-jvm-maven-deps-host:check")
        dependsOn(":examples:scripting-jvm-embeddable-host:check")
    }

    register("distTest") {
        dependsOn("compilerTest")
        dependsOn("frontendApiTests")
        dependsOn("toolsTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }

    register("specTest") {
        dependsOn("dist")
        dependsOn(":compiler:tests-spec:test")
    }

    register("androidCodegenTest") {
        dependsOn(":compiler:android-tests:test")
    }

    register("jps-tests") {
        dependsOn("dist")
        dependsOn(":jps:jps-plugin:test")
    }

    register("frontendApiTests") {
        dependsOn(":analysis:analysisAllTests")
    }

    register("kaptTests") {
        dependsOn(":kotlin-annotation-processing:test")
        dependsOn(":kotlin-annotation-processing:testJdk11")
        dependsOn(":kotlin-annotation-processing-base:test")
        dependsOn(":kotlin-annotation-processing-cli:test")
    }

    register("parcelizeTests") {
        dependsOn(":plugins:parcelize:parcelize-compiler:test")
    }

    register("codebaseTests") {
        dependsOn(":repo:codebase-tests:test")
    }

    register("statisticsTests") {
        dependsOn(":kotlin-gradle-statistics:test")
    }

    register("test") {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }

    named("check") {
        dependsOn("test")
    }

    register("dependenciesAll") {
        description = "Run dependencies task for all subprojects"
        // Cross-project dependsOn wiring is done in settings.gradle gradle.projectsEvaluated block.
    }

    named("checkBuild") {
        if (kotlinBuildProperties.isTeamcityBuild.get()) {
            val bootstrapKotlinVersion = bootstrapKotlinVersion
            doFirst {
                println("##teamcity[setParameter name='bootstrap.kotlin.version' value='$bootstrapKotlinVersion']")
            }
        }
    }

    register("publishGradlePluginArtifacts") {
        idePluginDependency {
            dependsOnKotlinGradlePluginPublish()
        }
    }

    register("publishIdeArtifacts") {
        idePluginDependency {
            @Suppress("UNCHECKED_CAST")
            dependsOn((rootProject.extra["compilerArtifactsForIde"] as List<String>).map { "$it:publish" })
        }
    }

    register("installIdeArtifacts") {
        idePluginDependency {
            @Suppress("UNCHECKED_CAST")
            dependsOn((rootProject.extra["compilerArtifactsForIde"] as List<String>).map { "$it:install" })
        }
    }

    register<Exec>("mvnInstall") {
        group = "publishing"
        workingDir = rootProject.projectDir.resolve("libraries")
        commandLine = getMvnwCmd() + listOf("clean", "install", "-DskipTests")
        doFirst {
            environment("JDK_1_8", getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8).get())
        }
    }
    val mvnPublishTask = register<Exec>("mvnPublish") {
        group = "publishing"
        workingDir = rootProject.projectDir.resolve("libraries")
        commandLine = getMvnwCmd() + listOf(
            "clean", "deploy", "--activate-profiles=noTest,local-bootstrap",
            "-Dinvoker.skip=true", "-DskipTests",
            "-Ddeploy-snapshot-repo=local",
            "-Ddeploy-snapshot-url=file://${rootProject.projectDir.resolve("build/repo")}",
            "-Dlocal-bootstrap-url=file://${rootProject.projectDir.resolve("build/repo")}",
        )

        val jdkToolchain1_8 = getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
        doFirst {
            environment("JDK_1_8", jdkToolchain1_8.get())
        }
    }

    // 'mvnPublish' is required for local bootstrap
    if (!kotlinBuildProperties.isTeamcityBuild.get()) {
        register("publish") {
            group = "publishing"
            finalizedBy(mvnPublishTask)
            // Cross-project dependsOn wiring is done in settings.gradle gradle.projectsEvaluated block.
        }
    }

    register<Exec>("installJps") {
        // Cross-project dependsOn wiring (MavenPublishPlugin -> publishToMavenLocal) is done in settings.gradle.
        group = "publishing"
        workingDir = rootProject.projectDir.resolve("libraries")
        commandLine = getMvnwCmd() + listOf("clean", "install", "-DskipTests", "-DexcludeTestModules=true")
        val jdk8Home = getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
        doFirst {
            environment("JDK_1_8", jdk8Home.get())
        }
    }
}

val zipCompiler by tasks.registering(Zip::class) {
    dependsOn(dist)
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-compiler-$kotlinVersion.zip")

    from(distKotlinHomeDir)
    into("kotlinc")

    doLast {
        logger.lifecycle("Compiler artifacts packed to ${archiveFile.get().asFile.absolutePath}")
    }
}

fun Project.secureZipTask(zipTask: TaskProvider<Zip>): RegisteringDomainObjectDelegateProviderWithAction<out TaskContainer, Task> {
    val checkSumTask: TaskProvider<Checksum> = tasks.register("${zipTask.name}Checksum", Checksum::class) {
        dependsOn(zipTask)
        inputFiles.setFrom(zipTask.map { it.outputs.files.singleFile })
        outputDirectory.fileProvider(zipTask.map { it.outputs.files.singleFile.parentFile.resolve("checksums") })
        checksumAlgorithm.set(Checksum.Algorithm.SHA256)
    }

    // Don't use Copy task, because it declares the full destination directory as an output
    val copyChecksumTask = tasks.register("${zipTask.name}ChecksumCopy") {
        dependsOn(checkSumTask)

        val checksumFileName: Provider<String> = zipTask.map { "${it.outputs.files.singleFile.name}.sha256" }
        val checksumFile: Provider<RegularFile> = checkSumTask.map { it.outputDirectory.file(checksumFileName.get()).get() }
        val outputFile: Provider<File> = zipTask.map { it.outputs.files.singleFile.parentFile.resolve(checksumFileName.get()) }

        inputs.file(checksumFile)
        outputs.file(outputFile)

        doLast {
            checksumFile.get().asFile.copyTo(outputFile.get(), overwrite = true)
        }
    }

    val signTask = tasks.register("${zipTask.name}Sign", Sign::class) {
        description = "Signs the archive produced by the '" + zipTask.name + "' task."
        sign(zipTask.get())
    }

    return tasks.registering {
        dependsOn(copyChecksumTask)
        dependsOn(signTask)
    }
}

signing {
    useGpgCmd()
}

val zipCompilerWithSignature by secureZipTask(zipCompiler)

configure<IdeaModel> {
    module {
        excludeDirs.addAll(
            files(
                commonLocalDataDir,
                ".kotlin",
                "test.output",
                "dist",
                "tmp",
                "intellij",
            )
        )
    }
}

val disableVerificationTasks = providers.gradleProperty("kotlin.build.disable.verification.tasks")
    .orNull?.toBoolean() ?: false
if (disableVerificationTasks) {
    logger.info("Verification tasks are disabled because `kotlin.build.disable.verification.tasks` is true")
    gradle.taskGraph.whenReady {
        allTasks.forEach {
            if (it is VerificationTask) {
                logger.info("Task ${it.path} is disabled because `kotlin.build.disable.verification.tasks` is true")
                it.enabled = false
            }
        }
    }
}

gradle.taskGraph.whenReady(checkYarnAndNPMSuppressed)

plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class) {
    extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java) {
        npmInstallTaskProvider.configure {
            args += listOf("--network-concurrency", "1", "--mutex", "network")
        }
    }
}

if (kotlinBuildProperties.isCacheRedirectorEnabled.get()) {
    configureJsCacheRedirector()
}

afterEvaluate {
    checkExpectedGradlePropertyValues()
}

// workaround for KT-68482
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    notCompatibleWithConfigurationCache("KotlinNpmInstallTask is not compatible with Configuration Cache")
}
