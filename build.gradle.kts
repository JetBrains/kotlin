import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.crypto.checksum.Checksum
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask

buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    kotlinBootstrapFrom(BootstrapOption.BintrayBootstrap(kotlinBuildProperties.kotlinBootstrapVersion!!, cacheRedirectorEnabled))

    repositories {
        bootstrapKotlinRepo?.let(::maven)

        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        } else {
            maven("https://plugins.gradle.org/m2")
        }
    }

    // a workaround for kotlin compiler classpath in kotlin project: sometimes gradle substitutes
    // kotlin-stdlib external dependency with local project :kotlin-stdlib in kotlinCompilerClasspath configuration.
    // see also configureCompilerClasspath@
    val bootstrapCompilerClasspath by configurations.creating

    dependencies {
        bootstrapCompilerClasspath(kotlin("compiler-embeddable", bootstrapKotlinVersion))

        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.19")
        classpath(kotlin("gradle-plugin", bootstrapKotlinVersion))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.17")
    }
}

if (kotlinBuildProperties.buildScanServer != null) {
    apply(from = "gradle/buildScanUserData.gradle")
}

plugins {
    base
    idea
    id("jps-compatible")
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("org.gradle.crypto.checksum") version "1.2.0"
    signing
}

pill {
    excludedDirs(
        "out",
        "buildSrc/build",
        "buildSrc/prepare-deps/android-dx/build",
        "buildSrc/prepare-deps/intellij-sdk/build"
    )
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

val configuredJdks: List<JdkId> =
    getConfiguredJdks().also {
        it.forEach { jdkId ->
            logger.info("Using ${jdkId.majorVersion} home: ${jdkId.homeDir}")
        }
    }

val defaultSnapshotVersion: String by extra
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(
        findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
            if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else defaultSnapshotVersion
        } ?: buildNumber
)

val kotlinLanguageVersion by extra("1.4")

allprojects {
    group = "org.jetbrains.kotlin"
    version = kotlinVersion
}

extra["kotlin_root"] = rootDir

val jpsBootstrap by configurations.creating

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val ideaUltimateSandboxDir = "$commonLocalDataDir/ideaUltimateSandbox"
val artifactsDir = "$distDir/artifacts"
val ideaPluginDir = "$artifactsDir/ideaPlugin/Kotlin"
val ideaUltimatePluginDir = "$artifactsDir/ideaUltimatePlugin/Kotlin"

extra["ktorExcludesForDaemon"] = listOf(
    "org.jetbrains.kotlin" to "kotlin-reflect",
    "org.jetbrains.kotlin" to "kotlin-stdlib",
    "org.jetbrains.kotlin" to "kotlin-stdlib-common",
    "org.jetbrains.kotlin" to "kotlin-stdlib-jdk8",
    "org.jetbrains.kotlin" to "kotlin-stdlib-jdk7",
    "org.jetbrains.kotlinx" to "kotlinx-coroutines-jdk8",
    "org.jetbrains.kotlinx" to "kotlinx-coroutines-core",
    "org.jetbrains.kotlinx" to "kotlinx-coroutines-core-common"
)

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaUltimateSandboxDir"] = project.file(ideaUltimateSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["ideaUltimatePluginDir"] = project.file(ideaUltimatePluginDir)
extra["isSonatypeRelease"] = false

// Work-around necessary to avoid setting null javaHome. Will be removed after support of lazy task configuration
val jdkNotFoundConst = "JDK NOT FOUND"

if (isTeamcityBuild) {
    extra["JDK_16"] = jdkPath("1.6")
    extra["JDK_17"] = jdkPath("1.7")
} else {
    extra["JDK_16"] = jdkPath("1.6", "1.8")
    extra["JDK_17"] = jdkPath("1.7", "1.8")
}
extra["JDK_18"] = jdkPath("1.8")
extra["JDK_9"] = jdkPath("9")
extra["JDK_10"] = jdkPath("10")
extra["JDK_11"] = jdkPath("11")

// allow opening the project without setting up all env variables (see KT-26413)
if (!kotlinBuildProperties.isInIdeaSync) {
    checkJDK()
}

fun checkJDK() {
    val missingEnvVars = JdkMajorVersion.values()
        .filter { it.isMandatory() && extra[it.name] == jdkNotFoundConst }
        .mapTo(ArrayList()) { it.name }

    if (missingEnvVars.isNotEmpty()) {
        throw GradleException("Required environment variables are missing: ${missingEnvVars.joinToString()}")
    }
}

rootProject.apply {
    from(rootProject.file("gradle/versions.gradle.kts"))
    from(rootProject.file("gradle/report.gradle.kts"))
    from(rootProject.file("gradle/javaInstrumentation.gradle.kts"))
    from(rootProject.file("gradle/jps.gradle.kts"))
    from(rootProject.file("gradle/checkArtifacts.gradle.kts"))
    from(rootProject.file("gradle/checkCacheability.gradle.kts"))
    from(rootProject.file("gradle/retryPublishing.gradle.kts"))
}

IdeVersionConfigurator.setCurrentIde(project)

extra["versions.protobuf"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.jansi"] = "1.16"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"
extra["versions.android"] = "2.3.1"
val coroutinesVersion = if (Platform[192].orHigher()) "1.3.7" else "1.1.1"
extra["versions.kotlinx-coroutines-core"] = coroutinesVersion
extra["versions.kotlinx-coroutines-jdk8"] = coroutinesVersion
extra["versions.json"] = "20160807"
extra["versions.native-platform"] = "0.14"
extra["versions.ant-launcher"] = "1.8.0"
extra["versions.robolectric"] = "4.0"
extra["versions.org.springframework"] = "4.2.0.RELEASE"
extra["versions.jflex"] = "1.7.0"
extra["versions.markdown"] = "0.1.25"
extra["versions.trove4j"] = "1.0.20181211"
extra["versions.completion-ranking-kotlin"] = "0.1.2"
extra["versions.r8"] = "2.0.88"
val immutablesVersion = "0.3.1"
extra["versions.kotlinx-collections-immutable"] = immutablesVersion
extra["versions.kotlinx-collections-immutable-jvm"] = immutablesVersion

// NOTE: please, also change KTOR_NAME in pathUtil.kt and all versions in corresponding jar names in daemon tests.
extra["versions.ktor-network"] = "1.0.1"

if (!project.hasProperty("versions.kotlin-native")) {
    extra["versions.kotlin-native"] = "1.4.20-dev-16314"
}

val intellijUltimateEnabled by extra(project.kotlinBuildProperties.intellijUltimateEnabled)
val effectSystemEnabled by extra(project.getBooleanProperty("kotlin.compiler.effectSystemEnabled") ?: false)
val newInferenceEnabled by extra(project.getBooleanProperty("kotlin.compiler.newInferenceEnabled") ?: false)
val useJvmIrBackend by extra(project.getBooleanProperty("kotlin.build.useIR") ?: false)

val intellijSeparateSdks = project.getBooleanProperty("intellijSeparateSdks") ?: false

extra["intellijSeparateSdks"] = intellijSeparateSdks

extra["IntellijCoreDependencies"] =
    listOf(
        when {
            Platform[202].orHigher() -> "asm-all-8.0.1"
            else -> "asm-all-7.0.1"
        },
        "guava",
        "jdom",
        "jna",
        "log4j",
        if (Platform[201].orHigher()) null else "picocontainer",
        "snappy-in-java",
        "streamex",
        "trove4j"
    ).filterNotNull()


extra["compilerModules"] = arrayOf(
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":compiler:container",
    ":compiler:resolution.common",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:psi",
    ":compiler:frontend",
    ":compiler:frontend.common",
    ":compiler:frontend.java",
    ":compiler:cli-common",
    ":compiler:ir.tree",
    ":compiler:ir.tree.impl",
    ":compiler:ir.tree.persistent",
    ":compiler:ir.psi2ir",
    ":compiler:ir.backend.common",
    ":compiler:backend.jvm",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.js",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.interpreter",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":compiler:backend-common",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:light-classes",
    ":compiler:javac-wrapper",
    ":compiler:cli",
    ":compiler:cli-js",
    ":compiler:incremental-compilation-impl",
    ":compiler:compiler.version",
    ":js:js.ast",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.config",
    ":js:js.frontend",
    ":js:js.translator",
    ":js:js.dce",
    ":native:frontend.native",
    ":native:kotlin-native-utils",
    ":compiler",
    ":kotlin-build-common",
    ":core:metadata",
    ":core:metadata.jvm",
    ":core:compiler.common",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":core:descriptors.runtime",
    ":core:deserialization",
    ":core:util.runtime",
    ":compiler:fir:cones",
    ":compiler:fir:resolve",
    ":compiler:fir:fir-serialization",
    ":compiler:fir:fir-deserialization",
    ":compiler:fir:tree",
    ":compiler:fir:raw-fir:raw-fir.common",
    ":compiler:fir:raw-fir:psi2fir",
    ":compiler:fir:raw-fir:light-tree2fir",
    ":compiler:fir:fir2ir",
    ":compiler:fir:fir2ir:jvm-backend",
    ":compiler:fir:java",
    ":compiler:fir:jvm",
    ":compiler:fir:checkers",
    ":compiler:fir:analysis-tests"
)

extra["compilerModulesForJps"] = listOf(
    ":kotlin-build-common",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":compiler:cli-common",
    ":kotlin-compiler-runner",
    ":daemon-common",
    ":daemon-common-new",
    ":core:compiler.common",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":idea:idea-jps-common",
    ":kotlin-preloader",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":js:js.config",
    ":core:util.runtime",
    ":compiler:compiler.version"
)

val coreLibProjects = listOfNotNull(
    ":kotlin-stdlib",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib-js",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-test:kotlin-test-annotations-common",
    ":kotlin-test:kotlin-test-common",
    ":kotlin-test:kotlin-test-jvm",
    ":kotlin-test:kotlin-test-junit",
    ":kotlin-test:kotlin-test-junit5",
    ":kotlin-test:kotlin-test-testng",
    ":kotlin-test:kotlin-test-js".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
    ":kotlin-reflect",
    ":kotlin-coroutines-experimental-compat"
)

val gradlePluginProjects = listOf(
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin-api",
    ":kotlin-allopen",
    ":kotlin-annotation-processing-gradle",
    ":kotlin-noarg",
    ":kotlin-sam-with-receiver"
)

apply {
    from("libraries/commonConfiguration.gradle")
    from("libraries/configureGradleTools.gradle")
}

apply {
    if (extra["isSonatypeRelease"] as? Boolean == true) {
        logger.info("Applying configuration for sonatype release")
        from("libraries/prepareSonatypeStaging.gradle")
    }
}

fun Task.listConfigurationContents(configName: String) {
    doFirst {
        project.configurations.findByName(configName)?.let {
            println("$configName configuration files:\n${it.allArtifacts.files.files.joinToString("\n  ", "  ")}")
        }
    }
}

val defaultJvmTarget = "1.8"
val defaultJavaHome = jdkPath(defaultJvmTarget)
val ignoreTestFailures by extra(project.kotlinBuildProperties.ignoreTestFailures)

allprojects {

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

    jvmTarget = defaultJvmTarget
    javaHome = defaultJavaHome

    // There are problems with common build dir:
    //  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
    //  - idea seems unable to exclude common buildDir from indexing
    // therefore it is disabled by default
    // buildDir = File(commonBuildDir, project.name)

    val mirrorRepo: String? = findProperty("maven.repository.mirror")?.toString()

    repositories {
        kotlinBuildLocalRepo(project)
        mirrorRepo?.let(::maven)
        jcenter()
        maven(protobufRepo)
        maven(intellijRepo)
        maven("https://dl.bintray.com/kotlin/ktor")
        maven("https://kotlin.bintray.com/kotlin-dependencies")
        maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
        maven("https://dl.google.com/dl/android/maven2")
        bootstrapKotlinRepo?.let(::maven)
        internalBootstrapRepo?.let(::maven)
    }

    configureJvmProject(javaHome!!, jvmTarget!!)

    val commonCompilerArgs = listOfNotNull(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xread-deserialized-contracts",
        "-progressive".takeIf { hasProperty("test.progressive.mode") }
    )

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            languageVersion = kotlinLanguageVersion
            apiVersion = kotlinLanguageVersion
            freeCompilerArgs = commonCompilerArgs
        }
    }

    val jvmCompilerArgs = listOf(
        "-Xjvm-default=compatibility",
        "-Xno-optimized-callable-references",
        "-Xno-kotlin-nothing-value-exception",
        "-Xnormalize-constructor-calls=enable"
    )

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
        kotlinOptions {
            freeCompilerArgs = commonCompilerArgs + jvmCompilerArgs

            if (useJvmIrBackend) {
                useIR = true
            }
        }
    }

    tasks.withType(VerificationTask::class.java as Class<Task>) {
        (this as VerificationTask).ignoreFailures = ignoreTestFailures
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    tasks.withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.withType<Test> {
        outputs.doNotCacheIf("https://youtrack.jetbrains.com/issue/KT-37089") { true }
    }

    tasks.withType<SourceTask>().configureEach {
        doFirst {
            source.visit {
                if (file.isDirectory && file.listFiles()?.isEmpty() == true) {
                    logger.warn("Empty source directories may cause build cache misses: " + file.absolutePath)
                }
            }
        }
    }

    normalization {
        runtimeClasspath {
            ignore("META-INF/MANIFEST.MF")
            ignore("META-INF/compiler.version")
            ignore("META-INF/plugin.xml")
            ignore("kotlin/KotlinVersionCurrentValue.class")
        }
    }

    tasks {
        register("listArchives") { listConfigurationContents("archives") }

        register("listRuntimeJar") { listConfigurationContents("runtimeJar") }

        register("listDistJar") { listConfigurationContents("distJar") }

        // Aggregate task for build related checks
        register("checkBuild")
    }

    afterEvaluate {
        if (javaHome != defaultJavaHome || jvmTarget != defaultJvmTarget) {
            logger.info("configuring project $name to compile to the target jvm version $jvmTarget using jdk: $javaHome")
            configureJvmProject(javaHome!!, jvmTarget!!)
        } // else we will actually fail during the first task execution. We could not fail before configuration is done due to impact on import in IDE

        fun File.toProjectRootRelativePathOrSelf() = (relativeToOrNull(rootDir)?.takeUnless { it.startsWith("..") } ?: this).path

        fun FileCollection.printClassPath(role: String) =
            println("${project.path} $role classpath:\n  ${joinToString("\n  ") { it.toProjectRootRelativePathOrSelf() }}")

        try {
            javaPluginConvention()
        } catch (_: UnknownDomainObjectException) {
            null
        }?.let { javaConvention ->
            tasks {
                register("printCompileClasspath") { doFirst { javaConvention.sourceSets["main"].compileClasspath.printClassPath("compile") } }
                register("printRuntimeClasspath") { doFirst { javaConvention.sourceSets["main"].runtimeClasspath.printClassPath("runtime") } }
                register("printTestCompileClasspath") { doFirst { javaConvention.sourceSets["test"].compileClasspath.printClassPath("test compile") } }
                register("printTestRuntimeClasspath") { doFirst { javaConvention.sourceSets["test"].runtimeClasspath.printClassPath("test runtime") } }
            }
        }

        run configureCompilerClasspath@{
            val bootstrapCompilerClasspath by rootProject.buildscript.configurations
            configurations.findByName("kotlinCompilerClasspath")?.let {
                dependencies.add(it.name, files(bootstrapCompilerClasspath))
            }

            configurations.findByName("kotlinCompilerPluginClasspath")
                ?.exclude("org.jetbrains.kotlin", "kotlin-scripting-compiler-embeddable")
        }

        apply(from = "$rootDir/gradle/cacheRedirector.gradle.kts")
    }
}

gradle.taskGraph.whenReady {
    fun Boolean.toOnOff(): String = if (this) "on" else "off"
    val profile = if (isTeamcityBuild) "CI" else "Local"

    val proguardMessage = "proguard is ${kotlinBuildProperties.proguard.toOnOff()}"
    val jarCompressionMessage = "jar compression is ${kotlinBuildProperties.jarCompression.toOnOff()}"
                val profileMessage = "$profile build profile is active ($proguardMessage, $jarCompressionMessage). " +
            "Use -Pteamcity=<true|false> to reproduce CI/local build"

    logger.warn("\n\n$profileMessage")

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

val syncMutedTests = tasks.register("syncMutedTests") {
    dependsOn(":compiler:tests-mutes:run")
}

val copyCompilerToIdeaPlugin by task<Copy> {
    dependsOn(dist)
    into(ideaPluginDir)
    from(distDir) { include("kotlinc/**") }
}

val ideaPlugin by task<Task> {
    dependsOn(copyCompilerToIdeaPlugin)
    dependsOn(":prepare:idea-plugin:ideaPlugin")
}

tasks {
    named("clean") {
        doLast {
            delete("$buildDir/repo")
            delete(distDir)
        }
    }

    register("cleanupArtifacts") {
        doLast {
            delete(artifactsDir)
        }
    }

    listOf("clean", "assemble", "install").forEach { taskName ->
        register("coreLibs${taskName.capitalize()}") {
            coreLibProjects.forEach { projectName -> dependsOn("$projectName:$taskName") }
        }
    }

    register("coreLibsTest") {
        (coreLibProjects + listOf(
            ":kotlin-stdlib:samples",
            ":kotlin-stdlib-js-ir",
            ":kotlin-test:kotlin-test-js-ir".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
            ":kotlin-test:kotlin-test-js:kotlin-test-js-it".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
            ":kotlinx-metadata-jvm",
            ":tools:binary-compatibility-validator"
        )).forEach {
            dependsOn("$it:check")
        }
    }

    register("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn("$it:check")
        }
    }

    register("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    register("jvmCompilerTest") {
        dependsOn("dist")
        dependsOn(
            ":compiler:test",
            ":compiler:container:test",
            ":compiler:tests-java8:test",
            ":compiler:tests-spec:test",
            ":compiler:tests-against-klib:test"
        )
        dependsOn(":plugins:jvm-abi-gen:test")
    }

    register("jvmCompilerIntegrationTest") {
        dependsOn(
            ":kotlin-compiler-embeddable:test",
            ":kotlin-compiler-client-embeddable:test"
        )
    }

    register("jsCompilerTest") {
        dependsOn(":js:js.tests:test")
        dependsOn(":js:js.tests:runMocha")
    }

    register("wasmCompilerTest") {
//  TODO: fix once
//        dependsOn(":js:js.tests:wasmTest")
    }

    register("nativeCompilerTest") {
        dependsOn(":native:kotlin-native-utils:test")
    }

    register("firCompilerTest") {
        dependsOn(":compiler:fir:raw-fir:psi2fir:test")
        dependsOn(":compiler:fir:raw-fir:light-tree2fir:test")
        dependsOn(":compiler:fir:analysis-tests:test")
        dependsOn(":compiler:fir:fir2ir:test")
    }

    register("firAllTest") {
        dependsOn(
            ":dist",
            ":compiler:fir:raw-fir:psi2fir:test",
            ":compiler:fir:raw-fir:light-tree2fir:test",
            ":compiler:fir:analysis-tests:test",
            ":compiler:fir:fir2ir:test",
            ":plugins:fir:fir-plugin-prototype:test"
        )
    }

    register("compilerFrontendVisualizerTest") {
        dependsOn("compiler:visualizer:test")
    }

    register("scriptingTest") {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
        dependsOn(":kotlin-scripting-compiler-embeddable:test")
        dependsOn(":kotlin-scripting-common:test")
        dependsOn(":kotlin-scripting-jvm:test")
        dependsOn(":kotlin-scripting-jvm-host-test:test")
        dependsOn(":kotlin-scripting-dependencies:test")
        dependsOn(":kotlin-scripting-dependencies-maven:test")
        dependsOn(":kotlin-scripting-jsr223-test:test")
        // see comments on the task in kotlin-scripting-jvm-host-test
//        dependsOn(":kotlin-scripting-jvm-host-test:embeddableTest")
        dependsOn(":kotlin-scripting-jsr223-test:embeddableTest")
        dependsOn(":kotlin-main-kts-test:test")
        dependsOn(":kotlin-scripting-ide-services-test:test")
        dependsOn(":kotlin-scripting-ide-services-test:embeddableTest")
        dependsOn(":kotlin-scripting-js-test:test")
    }

    register("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")
        dependsOn("miscCompilerTest")
    }

    register("miscCompilerTest") {
        dependsOn("wasmCompilerTest")
        dependsOn("nativeCompilerTest")
        dependsOn("firCompilerTest")

        dependsOn(":kotlin-daemon-tests:test")
        dependsOn("scriptingTest")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":core:descriptors.runtime:test")

        dependsOn("jvmCompilerIntegrationTest")
    }

    register("toolsTest") {
        dependsOn(":tools:kotlinp:test")
        dependsOn(":native:kotlin-klib-commonizer:test")
    }

    register("examplesTest") {
        dependsOn("dist")
        (project(":examples").subprojects + project(":kotlin-gradle-subplugin-example")).forEach { p ->
            dependsOn("${p.path}:check")
        }
    }

    register("distTest") {
        dependsOn("compilerTest")
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
        dependsOn(":jps-plugin:test")
    }

    register("idea-plugin-additional-tests") {
        dependsOn("dist")
        dependsOn(
            ":idea:idea-maven:test",
            ":j2k:test",
            ":nj2k:test",
            ":idea:jvm-debugger:jvm-debugger-core:test",
            ":idea:jvm-debugger:jvm-debugger-evaluation:test",
            ":idea:jvm-debugger:jvm-debugger-sequence:test",
            ":idea:jvm-debugger:eval4j:test",
            ":idea:scripting-support:test"
        )
    }

    if (Ide.IJ()) {
        register("idea-new-project-wizard-tests") {
            dependsOn("dist")
            dependsOn(
                ":libraries:tools:new-project-wizard:test",
                ":libraries:tools:new-project-wizard:new-project-wizard-cli:test",
                ":idea:idea-new-project-wizard:test"
            )
        }
    }

    register("idea-plugin-performance-tests") {
        dependsOn("dist")
        dependsOn(
            ":idea:performanceTests:performanceTest"
        )
    }

    register("android-ide-tests") {
        dependsOn("dist")
        dependsOn(
            ":plugins:android-extensions-ide:test",
            ":idea:idea-android:test",
            ":kotlin-annotation-processing:test"
        )
    }

    register("ideaPluginTest") {
        dependsOn(
            "mainIdeTests",
            "gradleIdeTest",
            "kaptIdeTest",
            "miscIdeTests"
        )
    }

    register("mainIdeTests") {
        dependsOn(":idea:test")
    }

    register("miscIdeTests") {
        dependsOn(
            ":kotlin-allopen-compiler-plugin:test",
            ":kotlin-noarg-compiler-plugin:test",
            ":kotlin-sam-with-receiver-compiler-plugin:test",
            ":plugins:uast-kotlin:test",
            ":kotlin-annotation-processing-gradle:test",
            ":kotlinx-serialization-compiler-plugin:test",
            ":kotlinx-serialization-ide-plugin:test",
            ":idea:jvm-debugger:jvm-debugger-test:test",
            "idea-plugin-additional-tests",
            "jps-tests",
            ":generators:test"
        )
        if (Ide.IJ()) {
            dependsOn("idea-new-project-wizard-tests")
        }
    }

    register("kaptIdeTest") {
        dependsOn(":kotlin-annotation-processing:test")
    }

    register("gradleIdeTest") {
        dependsOn(
            ":idea:idea-gradle:test",
            ":idea:idea-gradle-native:test"
        )
    }

    register("kmmTest", AggregateTest::class) {
        dependsOn(
            ":idea:idea-gradle:test",
            ":idea:test",
            ":compiler:test",
            ":js:js.tests:test"
        )
        if (Ide.IJ193.orHigher())
            dependsOn(":kotlin-gradle-plugin-integration-tests:test")
        if (Ide.AS40.orHigher())
            dependsOn(":kotlin-ultimate:ide:android-studio-native:test")

        testPatternFile = file("tests/mpp/kmm-patterns.csv")
    }

    register("test") {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }

    named("check") {
        dependsOn("test")
    }

    named("checkBuild") {
        if (kotlinBuildProperties.isTeamcityBuild) {
            doFirst {
                println("##teamcity[setParameter name='bootstrap.kotlin.version' value='$bootstrapKotlinVersion']")
            }
        }
    }

    register("publishIdeArtifacts") {
        idePluginDependency {
            dependsOn(
                ":prepare:ide-plugin-dependencies:android-extensions-compiler-plugin-for-ide:publish",
                ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide:publish",
                ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-tests-for-ide:publish",
                ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide:publish",
                ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide:publish",
                ":prepare:ide-plugin-dependencies:kotlin-compiler-for-ide:publish",
                ":prepare:ide-plugin-dependencies:kotlin-dist-for-ide:publish",
                ":prepare:ide-plugin-dependencies:kotlin-gradle-statistics-for-ide:publish",
                ":prepare:ide-plugin-dependencies:kotlinx-serialization-compiler-plugin-for-ide:publish",
                ":prepare:ide-plugin-dependencies:noarg-compiler-plugin-for-ide:publish",
                ":prepare:ide-plugin-dependencies:sam-with-receiver-compiler-plugin-for-ide:publish",
                ":prepare:ide-plugin-dependencies:compiler-components-for-jps:publish",
                ":kotlin-script-runtime:publish",
                ":kotlin-script-util:publish",
                ":kotlin-scripting-common:publish",
                ":kotlin-scripting-jvm:publish",
                ":kotlin-scripting-compiler:publish",
                ":kotlin-scripting-compiler-impl:publish",
                ":kotlin-android-extensions-runtime:publish",
                ":kotlin-stdlib-common:publish",
                ":kotlin-stdlib:publish",
                ":kotlin-stdlib-jdk7:publish",
                ":kotlin-stdlib-jdk8:publish",
                ":kotlin-reflect:publish",
                ":kotlin-main-kts:publish",
                ":kotlin-stdlib-js:publish",
                ":kotlin-test:kotlin-test-js:publish"
            )
        }
    }
}

fun CopySpec.setExecutablePermissions() {
    filesMatching("**/bin/*") { mode = 0b111101101 }
    filesMatching("**/bin/*.bat") { mode = 0b110100100 }
}

val zipCompiler by task<Zip> {
    dependsOn(dist)
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-compiler-$kotlinVersion.zip")

    from(distKotlinHomeDir)
    into("kotlinc")
    setExecutablePermissions()

    doLast {
        logger.lifecycle("Compiler artifacts packed to ${archiveFile.get().asFile.absolutePath}")
    }
}

val zipStdlibTests by task<Zip> {
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-stdlib-tests.zip")
    from("libraries/stdlib/common/test") { into("common") }
    from("libraries/stdlib/test") { into("test") }
    from("libraries/kotlin.test/common/src/test/kotlin") { into("kotlin-test") }
    doLast {
        logger.lifecycle("Stdlib tests are packed to ${archiveFile.get()}")
    }
}

val zipTestData by task<Zip> {
    dependsOn(zipStdlibTests)
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-test-data.zip")
    isZip64 = true
    from("compiler/testData") { into("compiler") }
    from("idea/testData") { into("ide") }
    from("idea/idea-completion/testData") { into("ide/completion") }
    from("compiler/tests-common/tests/org/jetbrains/kotlin/coroutineTestUtil.kt") { into("compiler") }
    doLast {
        logger.lifecycle("Test data packed to ${archiveFile.get()}")
    }
}

val zipPlugin by task<Zip> {
    val src = when (project.findProperty("pluginArtifactDir") as String?) {
        "Kotlin" -> ideaPluginDir
        "KotlinUltimate" -> ideaUltimatePluginDir
        null -> if (project.hasProperty("ultimate")) ideaUltimatePluginDir else ideaPluginDir
        else -> error("Unsupported plugin artifact dir")
    }
    val destPath = project.findProperty("pluginZipPath") as String?
    val dest = File(destPath ?: "$buildDir/kotlin-plugin.zip")
    destinationDirectory.set(dest.parentFile)
    archiveFileName.set(dest.name)

    from(src)
    into("Kotlin")
    setExecutablePermissions()

    doLast {
        logger.lifecycle("Plugin artifacts packed to $archiveFile")
    }
}

fun Project.secureZipTask(zipTask: TaskProvider<Zip>): RegisteringDomainObjectDelegateProviderWithAction<out TaskContainer, Task> {
    val checkSumTask = tasks.register("${zipTask.name}Checksum", Checksum::class) {
        dependsOn(zipTask)
        val compilerFile = zipTask.get().outputs.files.singleFile
        files = files(compilerFile)
        outputDir = compilerFile.parentFile
        algorithm = Checksum.Algorithm.SHA256
    }

    val signTask = tasks.register("${zipTask.name}Sign", Sign::class) {
        description = "Signs the archive produced by the '" + zipTask.name + "' task."
        sign(zipTask.get())
    }

    return tasks.registering {
        dependsOn(checkSumTask)
        dependsOn(signTask)
    }
}

signing {
    useGpgCmd()
}

val zipCompilerWithSignature by secureZipTask(zipCompiler)
val zipPluginWithSignature by secureZipTask(zipPlugin)

configure<IdeaModel> {
    module {
        excludeDirs = files(
            project.buildDir,
            commonLocalDataDir,
            ".gradle",
            "dependencies",
            "dist",
            "tmp"
        ).toSet()
    }
}

fun jdkPathOrNull(version: String): String? {
    val jdkName = "JDK_${version.replace(".", "")}"
    val jdkMajorVersion = JdkMajorVersion.valueOf(jdkName)
    return configuredJdks.find { it.majorVersion == jdkMajorVersion }?.homeDir?.canonicalPath
}

fun jdkPath(version: String, vararg replacementVersions: String): String {
    return jdkPathOrNull(version) ?: run {
        replacementVersions.asSequence().map { jdkPathOrNull(it) }.find { it != null }
    } ?: jdkNotFoundConst
}

fun Project.configureJvmProject(javaHome: String, javaVersion: String) {
    val currentJavaHome = File(System.getProperty("java.home")!!).canonicalPath
    val shouldFork = !currentJavaHome.startsWith(File(javaHome).canonicalPath)

    tasks.withType<JavaCompile> {
        if (name != "compileJava9Java") {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
            options.isFork = shouldFork
            options.forkOptions.javaHome = file(javaHome)
            options.compilerArgs.add("-proc:none")
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jdkHome = javaHome
        kotlinOptions.jvmTarget = javaVersion
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
    }

    tasks.withType<Test> {
        executable = File(javaHome, "bin/java").canonicalPath
    }

    plugins.withId("java-base") {
        configureShadowJarSubstitutionInCompileClasspath()
    }
}

fun Project.configureShadowJarSubstitutionInCompileClasspath() {
    val substitutionMap = mapOf(":kotlin-reflect" to ":kotlin-reflect-api")

    fun configureSubstitution(substitution: DependencySubstitution) {
        val requestedProject = (substitution.requested as? ProjectComponentSelector)?.projectPath ?: return
        val replacementProject = substitutionMap[requestedProject] ?: return
        substitution.useTarget(project(replacementProject), "Non-default shadow jars should not be used in compile classpath")
    }

    sourceSets.all {
        for (configName in listOf(compileOnlyConfigurationName, compileClasspathConfigurationName)) {
            configurations.getByName(configName).resolutionStrategy.dependencySubstitution {
                all(::configureSubstitution)
            }
        }
    }
}

tasks.register("findShadowJarsInClasspath") {
    doLast {
        fun Collection<File>.printSorted(indent: String = "    ") {
            sortedBy { it.path }.forEach { println(indent + it.relativeTo(rootProject.projectDir)) }
        }

        val mainJars = hashSetOf<File>()
        val shadowJars = hashSetOf<File>()
        for (project in rootProject.allprojects) {
            project.withJavaPlugin {
                project.sourceSets.forEach { sourceSet ->
                    val jarTask = project.tasks.findByPath(sourceSet.jarTaskName) as? Jar
                    jarTask?.outputFile?.let { mainJars.add(it) }
                }
            }
            for (task in project.tasks) {
                when (task) {
                    is ShadowJar -> {
                        shadowJars.add(fileFrom(task.outputFile))
                    }
                    is ProGuardTask -> {
                        shadowJars.addAll(task.outputs.files.toList())
                    }
                }
            }
        }

        shadowJars.removeAll(mainJars)
        println("Shadow jars that might break incremental compilation:")
        shadowJars.printSorted()

        fun Project.checkConfig(configName: String) {
            val config = configurations.findByName(configName) ?: return
            val shadowJarsInConfig = config.resolvedConfiguration.files.filter { it in shadowJars }
            if (shadowJarsInConfig.isNotEmpty()) {
                println()
                println("Project $project contains shadow jars in configuration '$configName':")
                shadowJarsInConfig.printSorted()
            }
        }

        for (project in rootProject.allprojects) {
            project.sourceSetsOrNull?.forEach { sourceSet ->
                project.checkConfig(sourceSet.compileClasspathConfigurationName)
            }
        }
    }
}

val Jar.outputFile: File
    get() = archiveFile.get().asFile

val Project.sourceSetsOrNull: SourceSetContainer?
    get() = convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets

val disableVerificationTasks = System.getProperty("disable.verification.tasks") == "true"
if (disableVerificationTasks) {
    gradle.taskGraph.whenReady {
        allTasks.forEach {
            if (it is VerificationTask) {
                logger.info("DISABLED: '$it'")
                it.enabled = false
            }
        }
    }
}
