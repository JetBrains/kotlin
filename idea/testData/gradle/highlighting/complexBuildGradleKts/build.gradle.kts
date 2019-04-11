buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {

    }
}

plugins {
    application
    kotlin("jvm") version "1.2.70"
}

val defaultSnapshotVersion: String by extra("1.2.3")
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(findProperty("deployVersion")?.toString() ?: buildNumber)

val kotlinLanguageVersion by extra("1.3")

allprojects {
    group = "org.jetbrains.kotlin"
    version = kotlinVersion
}

extra["kotlin_root"] = rootDir

val cidrKotlinPlugin by configurations.creating
val appcodeKotlinPlugin by configurations.creating
val clionKotlinPlugin by configurations.creating

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val ideaUltimateSandboxDir = "$commonLocalDataDir/ideaUltimateSandbox"
val clionSandboxDir = "$commonLocalDataDir/clionSandbox"
val appcodeSandboxDir = "$commonLocalDataDir/appcodeSandbox"
val ideaPluginDir = "$distDir/artifacts/ideaPlugin/Kotlin"
val ideaUltimatePluginDir = "$distDir/artifacts/ideaUltimatePlugin/Kotlin"
val cidrPluginDir = "$distDir/artifacts/cidrPlugin/Kotlin"
val appcodePluginDir = "$distDir/artifacts/appcodePlugin/Kotlin"
val clionPluginDir = "$distDir/artifacts/clionPlugin/Kotlin"

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaUltimateSandboxDir"] = project.file(ideaUltimateSandboxDir)
extra["clionSandboxDir"] = project.file(ideaSandboxDir)
extra["appcodeSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["ideaUltimatePluginDir"] = project.file(ideaUltimatePluginDir)
extra["cidrPluginDir"] = project.file(cidrPluginDir)
extra["appcodePluginDir"] = project.file(appcodePluginDir)
extra["clionPluginDir"] = project.file(clionPluginDir)
extra["isSonatypeRelease"] = false

// Work-around necessary to avoid setting null javaHome. Will be removed after support of lazy task configuration
val jdkNotFoundConst = "JDK NOT FOUND"

extra["versions.protobuf"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.jansi"] = "1.16"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"
extra["versions.android"] = "2.3.1"
extra["versions.kotlinx-coroutines-core"] = "1.0.1"
extra["versions.kotlinx-coroutines-jdk8"] = "1.0.1"
extra["versions.json"] = "20160807"
extra["versions.native-platform"] = "0.14"
extra["versions.ant-launcher"] = "1.8.0"
extra["versions.robolectric"] = "3.1"
extra["versions.org.springframework"] = "4.2.0.RELEASE"
extra["versions.jflex"] = "1.7.0"
extra["versions.markdown"] = "0.1.25"
extra["versions.trove4j"] = "1.0.20181211"

val isTeamcityBuild = false
val intellijUltimateEnabled = false
val effectSystemEnabled by extra(false)
val newInferenceEnabled by extra(false)

val intellijSeparateSdks = false

extra["intellijUltimateEnabled"] = intellijUltimateEnabled
extra["intellijSeparateSdks"] = intellijSeparateSdks

extra["IntellijCoreDependencies"] =
    listOf("annotations",
           "asm-all",
           "guava",
           "jdom",
           "jna",
           "log4j",
           "picocontainer",
           "snappy-in-java",
           "streamex",
           "trove4j")


extra["compilerModules"] = arrayOf(
    ":compiler:util",
    ":compiler:container",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:psi",
    *if (project.findProperty("fir.enabled") == "true") {
        arrayOf(
            ":compiler:fir:cones",
            ":compiler:fir:resolve",
            ":compiler:fir:tree",
            ":compiler:fir:psi2fir",
            ":compiler:fir:fir2ir"
        )
    } else {
        emptyArray()
    },
    ":compiler:frontend",
    ":compiler:frontend.common",
    ":compiler:frontend.java",
    ":compiler:frontend.script",
    ":compiler:cli-common",
    ":compiler:daemon-common",
    ":compiler:daemon",
    ":compiler:ir.tree",
    ":compiler:ir.psi2ir",
    ":compiler:ir.backend.common",
    ":compiler:backend.jvm",
    ":compiler:backend.js",
    ":compiler:backend-common",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:light-classes",
    ":compiler:cli",
    ":compiler:incremental-compilation-impl",
    ":js:js.ast",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.frontend",
    ":js:js.translator",
    ":js:js.dce",
    ":compiler",
    ":kotlin-build-common",
    ":core:metadata",
    ":core:metadata.jvm",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":core:deserialization",
    ":core:util.runtime"
)

val coreLibProjects = listOf(
    ":kotlin-stdlib",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib-js",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-test:kotlin-test-common",
    ":kotlin-test:kotlin-test-jvm",
    ":kotlin-test:kotlin-test-junit",
    ":kotlin-test:kotlin-test-junit5",
    ":kotlin-test:kotlin-test-testng",
    ":kotlin-test:kotlin-test-js",
    ":kotlin-reflect"
)

val gradlePluginProjects = listOf(
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin:plugin-marker",
    ":kotlin-gradle-plugin-api",
//        ":kotlin-gradle-plugin-integration-tests",  // TODO: build fails
    ":kotlin-allopen",
    ":kotlin-allopen:plugin-marker",
    ":kotlin-annotation-processing-gradle",
    ":kotlin-noarg",
    ":kotlin-noarg:plugin-marker",
    ":kotlin-sam-with-receiver"
)

fun Task.listConfigurationContents(configName: String) {
    doFirst {
        project.configurations.findByName(configName)?.let {
            println("$configName configuration files:\n${it.allArtifacts.files.files.joinToString("\n  ", "  ")}")
        }
    }
}

val defaultJvmTarget = "1.8"
val defaultJavaHome = "home"
val ignoreTestFailures by extra(project.findProperty("ignoreTestFailures")?.toString()?.toBoolean() ?: project.hasProperty("teamcity"))

allprojects {

    if (defaultJavaHome != null) {
        logger.error("Could not find default java home. Please set environment variable JDK_${defaultJavaHome} to point to JDK ${defaultJavaHome} installation.")
    }


    // There are problems with common build dir:
    //  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
    //  - idea seems unable to exclude common builddir from indexing
    // therefore it is disabled by default
    // buildDir = File(commonBuildDir, project.name)

    val mirrorRepo: String? = findProperty("maven.repository.mirror")?.toString()

    repositories {
        jcenter()
    }


    val commonCompilerArgs = listOfNotNull(
        "-Xallow-kotlin-package",
        "-Xread-deserialized-contracts",
        "-Xjvm-default=compatibility",
        "-Xprogressive".takeIf { hasProperty("test.progressive.mode") } // TODO: change to "-progressive" after bootstrap
    )

    tasks.withType(VerificationTask::class.java as Class<Task>) {
        (this as VerificationTask).ignoreFailures = ignoreTestFailures
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    task<Jar>("javadocJar") {
        classifier = "javadoc"
    }

    tasks.withType<Jar> {
    }

    task("listArchives") { listConfigurationContents("archives") }

    task("listRuntimeJar") { listConfigurationContents("runtimeJar") }

    task("listDistJar") { listConfigurationContents("distJar") }

    afterEvaluate {
        fun File.toProjectRootRelativePathOrSelf() = (relativeToOrNull(rootDir)?.takeUnless { it.startsWith("..") } ?: this).path

        fun FileCollection.printClassPath(role: String) =
            println("${project.path} $role classpath:\n  ${joinToString("\n  ") { it.toProjectRootRelativePathOrSelf() } }")

        run configureCompilerClasspath@ {
            configurations.findByName("kotlinCompilerClasspath")?.let {
                dependencies.add(it.name, files(""))
            }
        }
    }
}

gradle.taskGraph.whenReady {

        logger.warn("Local build profile is active (IC is on, proguard is off). Use -Pteamcity=true to reproduce TC build")
        for (task in allTasks) {
            when (task) {
                // todo: remove when Gradle 4.10+ is used (Java IC on by default)
                is JavaCompile -> task.options.isIncremental = true
                is org.gradle.jvm.tasks.Jar -> task.entryCompression = ZipEntryCompression.STORED
            }
        }
}

tasks {

    create("cleanupArtifacts") {
        doLast {
            delete(ideaPluginDir)
            delete(ideaUltimatePluginDir)
            delete(cidrPluginDir)
            delete(appcodePluginDir)
            delete(clionPluginDir)
        }
    }

    listOf("clean", "assemble", "install", "dist").forEach { taskName ->
        create("coreLibs${taskName.capitalize()}") {
            coreLibProjects.forEach { projectName -> dependsOn("$projectName:$taskName") }
        }
    }

    create("coreLibsTest") {
        (coreLibProjects + listOf(
            ":kotlin-stdlib-jre7",
            ":kotlin-stdlib-jre8",
            ":kotlin-stdlib:samples",
            ":kotlin-test:kotlin-test-js:kotlin-test-js-it",
            ":kotlinx-metadata-jvm",
            ":tools:binary-compatibility-validator"
        )).forEach {
            dependsOn(it + ":check")
        }
    }

    create("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn(it + ":check")
        }
    }

    create("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    create("jvmCompilerTest") {
        dependsOn("dist")
        dependsOn(":compiler:test",
                  ":compiler:container:test",
                  ":compiler:tests-java8:test",
                  ":compiler:tests-spec:remoteRunTests")
        dependsOn(":plugins:jvm-abi-gen:test")
    }

    create("jsCompilerTest") {
        dependsOn(":js:js.tests:test")
        dependsOn(":js:js.tests:runMocha")
    }

    create("scriptingTest") {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
        dependsOn(":kotlin-scripting-jvm-host:test")
    }

    create("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")

        dependsOn("scriptingTest")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":core:descriptors.runtime:test")
    }

    create("toolsTest") {
        dependsOn(":tools:kotlinp:test")
    }

    create("examplesTest") {
        dependsOn("dist")
    }

    create("distTest") {
        dependsOn("compilerTest")
        dependsOn("toolsTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }

    create("specTest") {
        dependsOn("dist")
        dependsOn(":compiler:tests-spec:test")
    }

    create("androidCodegenTest") {
        dependsOn(":compiler:android-tests:test")
    }

    create("jps-tests") {
        dependsOn("dist")
        dependsOn(":jps-plugin:test")
    }

    create("idea-plugin-main-tests") {
        dependsOn("dist")
        dependsOn(":idea:test")
    }

    create("idea-plugin-additional-tests") {
        dependsOn("dist")
        dependsOn(":idea:idea-gradle:test",
                  ":idea:idea-maven:test",
                  ":j2k:test",
                  ":idea:eval4j:test")
    }

    create("idea-plugin-tests") {
        dependsOn("dist")
        dependsOn("idea-plugin-main-tests",
                  "idea-plugin-additional-tests")
    }

    create("android-ide-tests") {
        dependsOn("dist")
        dependsOn(":plugins:android-extensions-ide:test",
                  ":idea:idea-android:test",
                  ":kotlin-annotation-processing:test")
    }

    create("plugins-tests") {
        dependsOn("dist")
        dependsOn(":kotlin-annotation-processing:test",
                  ":kotlin-source-sections-compiler-plugin:test",
                  ":kotlin-allopen-compiler-plugin:test",
                  ":kotlin-noarg-compiler-plugin:test",
                  ":kotlin-sam-with-receiver-compiler-plugin:test",
                  ":plugins:uast-kotlin:test",
                  ":kotlin-annotation-processing-gradle:test",
                  ":kotlinx-serialization-ide-plugin:test")
    }


    create("ideaPluginTest") {
        dependsOn(
            "idea-plugin-tests",
            "jps-tests",
            "plugins-tests",
            "android-ide-tests",
            ":generators:test"
        )
    }


    create("mainTest") {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }

    create("mainC" +
                   "eck") {
        dependsOn("test")
    }
}