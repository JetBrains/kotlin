import org.gradle.crypto.checksum.Checksum
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.testFederation.TestFederationInferAffectedDomainsTask

buildscript {
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
    base
    idea
    alias(libs.plugins.jetbrains.ideaExt)
    id("build-time-report")
    id("modularized-test-configurations")
    id("resolve-dependencies")
    alias(libs.plugins.gradle.crypto.checksum)
    alias(libs.plugins.kotlinx.bcv) apply false
    id("signing-convention")
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        id("kotlin.native.build-tools-conventions") apply false
    }
    `jvm-toolchains`
    alias(libs.plugins.gradle.node) apply false
    id("gradle-plugins-documentation") apply false
    id("com.autonomousapps.dependency-analysis")
    id("project-tests-convention") apply false
    id("test-federation-convention") apply false
    id("nodejs-configuration") apply false
    id("d8-root-configuration")
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

findProperty("deployVersion")?.let {
    assert(findProperty("build.number") != null) { "`build.number` parameter is expected to be explicitly set with the `deployVersion`" }
}

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
}

pluginManager.apply("nodejs-configuration")

IdeVersionConfigurator.setCurrentIde(project)

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
val mppProjects by extra {
    listOf(
        ":kotlin-stdlib",
        ":kotlin-test",
    )
}

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

// The root project applies common configuration explicitly here, after its `extra` is populated.
pluginManager.apply("common-configuration")

gradle.taskGraph.whenReady {
    fun Boolean.toOnOff(): String = if (this) "on" else "off"
    val profile = if (isTeamcityBuild.get()) "CI" else "Local"

    val proguardMessage = "proguard is ${kotlinBuildProperties.proguard.toOnOff()}"
    val jarCompressionMessage = "jar compression is ${kotlinBuildProperties.jarCompression.toOnOff()}"

    logger.warn(
        "$profile build profile is active ($proguardMessage, $jarCompressionMessage). " +
                "Use -Pteamcity=<true|false> to reproduce CI/local build"
    )
}

val dist = tasks.register("dist") {
    dependsOn(":kotlin-compiler:dist")
}

val createIdeaHomeForTests = tasks.register("createIdeaHomeForTests") {
    val ideaBuildNumberFileForTests = ideaBuildNumberFileForTests()
    val intellijSdkVersion = kotlinBuildProperties.versionsProperty("intellijSdk").get()
    outputs.dir(ideaHomePathForTests())
    doFirst {
        with(ideaBuildNumberFileForTests.get().asFile) {
            parentFile.mkdirs()
            writeText("IC-$intellijSdkVersion")
        }
    }
}
val ideaHomeForTests = configurations.consumable("ideaHomeForTests")
artifacts {
    add(ideaHomeForTests.name, createIdeaHomeForTests)
}

val publishedMark: NamedDomainObjectProvider<DependencyScopeConfiguration> = configurations.dependencyScope("publishedMark")
val publishedMarkElements: NamedDomainObjectProvider<ResolvableConfiguration> = configurations.resolvable("publishedMarkClasspath").apply {
    configure { extendsFrom(publishedMark) }
}
val localPublishedMark: NamedDomainObjectProvider<DependencyScopeConfiguration> = configurations.dependencyScope("localPublishedMark")
val localPublishedMarkElements: NamedDomainObjectProvider<ResolvableConfiguration> = configurations.resolvable("localPublishedMarkClasspath").apply {
    configure { extendsFrom(publishedMark) }
}
dependencies {
    allprojects.forEach { p ->
        add(publishedMark.name, project(p.path, configuration = "publishedMark"))
        add(localPublishedMark.name, project(p.path, configuration = "localPublishedMark"))
    }
}

tasks {
    register("compileAll") {
        doNotTrackState("This is just a lifecycle task to compile all, we don't want to hash the inputs.")
        /*
         * Build cache tests don't work properly with KMP projects,
         * so such projects are temporarily excluded from them (KTI-2822)
         */
        val excludedNativePrefixes = listOf(
            ":native",
            ":libraries:tools:analysis-api-based-klib-reader:testProject",
            ":plugins:plugin-sandbox:plugin-annotations",
            ":kotlin-power-assert-runtime",
        )
        val projectsToRun = allprojects
            .filter {
                excludedNativePrefixes.none(it.path::startsWith) || kotlinBuildProperties.isKotlinNativeEnabled.get()
            }.map { it.path }
        val conf: FileCollection = configurations.detachedConfiguration(
            *projectsToRun.map {
                this.project.dependencies.project(it, configuration = "compileAll")
            }.toTypedArray()
        ).incoming.artifactView { lenient(true) }.files
        inputs.files(conf).withNormalizer(ClasspathNormalizer::class.java)
    }

    named<Delete>("clean") {
        delete(distDir)
        delete(layout.buildDirectory.dir("repo"))
    }

    register<Delete>("cleanupArtifacts") {
        delete = setOf(artifactsDir)
    }

    val coreLibsPublishable = coreLibProjects + listOf(":kotlin-stdlib-common")
    val coreLibsBuildable = coreLibProjects + listOf(":kotlin-stdlib-jvm-minimal-for-test", ":kotlin-stdlib-js-ir-minimal-for-test")

    register("coreLibsClean") {
        dependsOnAll(
            task = "clean",
            projects = coreLibProjects + coreLibsBuildable + coreLibsPublishable + ":kotlin-stdlib:samples"
        )
    }

    register("coreLibsAssemble") {
        dependsOnAll("assemble", coreLibsBuildable)
    }

    register("coreLibsInstall") {
        dependsOnAll("install", coreLibsPublishable)
    }

    register("coreLibsPublish") {
        dependsOnAll("publish", coreLibsPublishable)
    }

    testLifecycleTask("coreLibsTest") {
        dependsOnAll(
            task = "check",
            projects = coreLibsBuildable + listOf(
                ":kotlin-stdlib:samples",
                ":kotlin-test:kotlin-test-js-it",
                ":tools:binary-compatibility-validator",
                ":tools:jdk-api-validator",
            )
        )
    }

    testLifecycleTask("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn("$it:check")
        }
    }

    testLifecycleTask("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    testLifecycleTask("jvmCompilerTest") {
        dependsOn(
            ":compiler:tests-common-new:test",
            ":compiler:container:test",
            ":compiler:tests-java8:test",
            ":compiler:tests-spec:test",
        )
    }

    testLifecycleTask("testsForBootstrapBuildTest") {
        dependsOn(":compiler:tests-common-new:test")
    }

    testLifecycleTask("jvmCompilerIntegrationTest") {
        dependsOn(
            ":kotlin-compiler-embeddable:test",
            ":kotlin-compiler-client-embeddable:test"
        )
    }

    testLifecycleTask("nativeImageCompilerTest") {
        dependsOn(":kotlin-compiler-native-image:nativeImageBoxTest")
        dependsOn(":kotlin-compiler-native-image:nativeImageSmokeTest")
    }

    testLifecycleTask("jsCompilerTest") {
        dependsOn(":js:js.tests:jsTest")
    }

    testLifecycleTask("wasmCompilerTest") {
        // KTI-2670: TODO: don't invoke this obsolete task in KTI
    }

    testLifecycleTask("wasmFirCompilerTest") {
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
    testLifecycleTask("nativeCompilerTest") {
        dependsOn(":kotlin-atomicfu-compiler-plugin:nativeTest")
        dependsOn(":plugins:plugin-sandbox:nativeTest")
        dependsOn(":libraries:tools:analysis-api-based-klib-reader:check")
        dependsOn(":native:native.tests:test")
        dependsOn(":native:native.tests:cli-tests:check")
        dependsOn(":native:native.tests:codegen-box:check")
        dependsOn(":native:native.tests:driver:check")
        dependsOn(":native:native.tests:gc-fuzzing-tests:engine:check")
        dependsOn(":native:native.tests:stress:check")
        dependsOn(":native:native.tests:litmus-tests:check")
    }

    // Similar to nativeCompilerTest, but should be executed only on macOS host as these tests
    // technically or semantically depend on Xcode SDK.
    testLifecycleTask("nativeAppleSpecificTests") {
        dependsOn(":native:objcexport-header-generator:check")
        dependsOn(":native:swift:swift-export-embeddable:check")
        dependsOn(":native:swift:swift-export-standalone:check")
        dependsOn(":native:swift:swift-export-ide:test")
        dependsOn(":native:swift:sir-light-classes:check")
    }

    // These are unit tests of Native compiler
    testLifecycleTask("nativeCompilerUnitTest") {
        dependsOn(":native:kotlin-native-utils:check")
        dependsOn(":native:unsafe-mem:check")
        if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
            dependsOn(":kotlin-native:Interop:Indexer:check")
            dependsOn(":kotlin-native:Interop:StubGenerator:check")
            dependsOn(":kotlin-native:backend.native:check")
            dependsOn(":kotlin-native:tools:kdumputil:check")
            dependsOn(":kotlin-native:common:env:check")
            dependsOn(":kotlin-native:common:files:check")
            dependsOn(":kotlin-native:libclangInterop:check")
            dependsOn(":kotlin-native:libllvmext:check")
            dependsOn(":kotlin-native:llvmInterop:check")
        }
    }

    testLifecycleTask("klibIrTest") {
        dependsOn(":tools:binary-compatibility-validator:check")
        dependsOn(":native:native.tests:klib-ir-inliner:check")
    }

    testLifecycleTask("firCompilerTest") {
        dependsOn(":compiler:fir:raw-fir:psi2fir:test")
        dependsOn(":compiler:fir:raw-fir:light-tree2fir:test")
        dependsOn(":compiler:fir:analysis-tests:test")
        dependsOn(":compiler:fir:analysis-tests:legacy-fir-tests:test")
        dependsOn(":compiler:fir:fir2ir:aggregateTests")
    }

    testLifecycleTask("nightlyFirCompilerTest") {
        dependsOn(":compiler:fir:fir2ir:nightlyTests")
        dependsOn(":compiler:fastJarFSLongTests")
    }

    testLifecycleTask("scriptingJvmTest") {
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
        dependsOn(":kotlin-scripting-jsr223-test:test")
    }

    testLifecycleTask("scriptingTest") {
        dependsOn("scriptingJvmTest")
    }

    testLifecycleTask("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("miscCompilerTest")
    }

    testLifecycleTask("miscCompilerTest") {
        dependsOn(":compiler:test")
        dependsOn(":compiler:tests-integration:test")
        dependsOn(":compiler:java-direct:test")
        dependsOn(":kotlin-compiler-embeddable:test")
        dependsOn("incrementalCompilationTest")
        dependsOn("scriptingTest")
        dependsOn("jvmCompilerIntegrationTest")
        dependsOn("compilerPluginTest")
        dependsOn(":kotlin-daemon-tests:test")
        dependsOn(":compiler:arguments:check")
        dependsOn(":compiler:multiplatform-parsing:jvmTest")
        dependsOn(":compiler:fir:modularized-tests:test")
        dependsOn(":compiler:util:test")
        dependsOn(":core:names:check")
        dependsOn(":core:language.model:check")
        dependsOn(":core:language.targets:check")
        dependsOn(":core:language.targets.jvm:check")
        dependsOn(":core:language.version-settings:check")
        dependsOn(":core:language.version-settings:test")
    }

    testLifecycleTask("miscTest") {
        dependsOn("coreLibsTest")
        dependsOn("toolsTest")
        dependsOn("examplesTest")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":core:descriptors.runtime:test")
        dependsOn(":kotlin-util-io:test")
        dependsOn(":kotlin-util-klib:test")
        dependsOn(":kotlin-util-klib-abi:test")
        dependsOn(":kotlinx-metadata-klib:test")
        dependsOn(":compiler:ir.validation:test")
        dependsOn(":compiler:ir.serialization.js:test")
        dependsOn(":compiler:ir.serialization.native:test")
        dependsOn(":generators:test")
        dependsOn(":kotlin-gradle-plugin-dsl-codegen:test")
    }

    testLifecycleTask("incrementalCompilationTest") {
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":compiler:incremental-compilation-impl:testJvmICWithJdk11")
    }

    testLifecycleTask("compilerPluginTest") {
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

    testLifecycleTask("toolsTest") {
        dependsOn(":tools:kotlinp-jvm:test")
        dependsOn(":native:kotlin-klib-commonizer:test")
        dependsOn(":native:kotlin-klib-commonizer-api:test")
        dependsOn(":kotlin-tooling-core:check")
        dependsOn(":kotlin-tooling-metadata:check")
        dependsOn(":compiler:build-tools:kotlin-build-tools-api:check")
        dependsOn(":compiler:build-tools:kotlin-build-tools-api-tests:check")
        dependsOn(":compiler:build-tools:kotlin-build-tools-api-forward-tests:check")
        dependsOn(":tools:ide-plugin-dependencies-validator:test")
        dependsOn(":tools:stats-analyser:test")
        dependsOn(":libraries:tools:abi-validation:abi-tools:check")
        dependsOn(":libraries:tools:abi-validation:abi-tools-api:check")
        dependsOn(":libraries:tools:abi-validation:abi-tools-embeddable:check")
        dependsOn(":libraries:tools:abi-validation:abi-tools-tests:check")
    }

    testLifecycleTask("examplesTest") {
        dependsOn("dist")
        project(":examples").subprojects.forEach { p ->
            dependsOn("${p.path}:check")
        }
    }

    testLifecycleTask("distTest") {
        dependsOn("compilerTest")
        dependsOn("frontendApiTests")
        dependsOn("toolsTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }

    testLifecycleTask("specTest") {
        dependsOn("dist")
        dependsOn(":compiler:tests-spec:test")
    }

    testLifecycleTask("androidCodegenTest") {
        dependsOn(":compiler:android-tests:test")
    }

    testLifecycleTask("jps-tests") {
        dependsOn("dist")
        dependsOn(":jps:jps-plugin:test")
    }

    testLifecycleTask("frontendApiTests") {
        dependsOn(":analysis:analysisAllTests")
    }

    testLifecycleTask("kaptTests") {
        dependsOn(":kotlin-annotation-processing:test")
        dependsOn(":kotlin-annotation-processing:testJdk11")
        dependsOn(":kotlin-annotation-processing-base:test")
        dependsOn(":kotlin-annotation-processing-cli:test")
    }

    testLifecycleTask("parcelizeTests") {
        dependsOn(":plugins:parcelize:parcelize-compiler:test")
    }

    testLifecycleTask("codebaseTests") {
        dependsOn(":repo:auto-code-review:test")
        dependsOn(":repo:codebase-tests:test")
    }

    testLifecycleTask("statisticsTests") {
        dependsOn(":kotlin-gradle-statistics:test")
    }

    register("test") {
        doLast {
            throw GradleException("Don't use directly, use aggregate '*Test' tasks instead")
        }
    }

    named("check") {
        dependsOn("test")
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
        idePluginPublishingLatch {
            dependsOnKotlinGradlePluginPublish()
        }
    }

    fun registerSpecialPublishingTasks(nameSuffix: String, artifactProjectList: List<String>, latch: Project.(() -> Unit) -> Unit) {
        register("publish$nameSuffix") {
            latch {
                @Suppress("UNCHECKED_CAST")
                dependsOn(artifactProjectList.map { "$it:publish" })
            }
        }

        register("install$nameSuffix") {
            latch {
                @Suppress("UNCHECKED_CAST")
                dependsOn(artifactProjectList.map { "$it:install" })
            }
        }
    }

    registerSpecialPublishingTasks(
        nameSuffix = "IdeArtifacts",
        artifactProjectList = @Suppress("UNCHECKED_CAST") (CompilerModules.compilerArtifactsForIde),
        latch = Project::idePluginPublishingLatch
    )

    registerSpecialPublishingTasks(
        nameSuffix = "AnalysisApiArtifacts",
        artifactProjectList = @Suppress("UNCHECKED_CAST") (CompilerModules.analysisApiArtifacts),
        latch = Project::analysisApiPublishingLatch
    )

    register<Exec>("mvnInstall") {
        group = "publishing"
        workingDir = rootProject.projectDir.resolve("libraries")
        commandLine = getMvnwCmd() + listOf("clean", "install", "-DskipTests")
        inputs.files(localPublishedMarkElements.get().incoming.artifactView { lenient(true) }.files)
            .withPathSensitivity(PathSensitivity.NONE)
            .withPropertyName("localPublishedMarks")
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
            inputs.files(publishedMarkElements.get().incoming.artifactView { lenient(true) }.files)
                .withPathSensitivity(PathSensitivity.NONE)
                .withPropertyName("publishedMarks")
            finalizedBy(mvnPublishTask)
        }
    }

    register<Exec>("installJps") {
        inputs.files(localPublishedMarkElements.get().incoming.artifactView { lenient(true) }.files)
            .withPathSensitivity(PathSensitivity.NONE)
            .withPropertyName("localPublishedMarks")
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
    archiveFileName.set("kotlin-compiler-${project.version}.zip")

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

plugins.withType<YarnPlugin> {
    extensions.configure<YarnRootEnvSpec> {
        version = libs.versions.yarn
        downloadBaseUrl.set(null as String?)
    }
}

plugins.withType<WasmYarnPlugin> {
    extensions.configure<WasmYarnRootEnvSpec> {
        version = libs.versions.yarn
        downloadBaseUrl.set(null as String?)
    }
}

tasks.register<TestFederationInferAffectedDomainsTask>("inferAffectedDomains")
