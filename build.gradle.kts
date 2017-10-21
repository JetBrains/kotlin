import org.gradle.api.Project
import java.util.*
import java.io.File
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

buildscript {
    extra["defaultSnapshotVersion"] = "1.2-SNAPSHOT"

    kotlinBootstrapFrom(BootstrapOption.TeamCity("1.2.0-dev-92", onlySuccessBootstrap = false))

    val repos = listOfNotNull(
            bootstrapKotlinRepo,
            "https://jcenter.bintray.com/",
            "https://plugins.gradle.org/m2",
            "http://repository.jetbrains.com/utils/")

    extra["repos"] = repos

    extra["versions.shadow"] = "2.0.1"

    repositories {
        for (repo in repos) {
            maven { setUrl(repo) }
        }
    }

    dependencies {
        classpath("com.gradle.publish:plugin-publish-plugin:0.9.7")
        classpath(kotlinDep("gradle-plugin", bootstrapKotlinVersion))
    }
}

plugins {
    `build-scan`
    idea
}

buildScan {
    setLicenseAgreementUrl("https://gradle.com/terms-of-service")
    setLicenseAgree("yes")
}

val configuredJdks: List<JdkId> =
        getConfiguredJdks().also {
            it.forEach {
                logger.info("Using ${it.majorVersion} home: ${it.homeDir}")
            }
        }

val defaultSnapshotVersion: String by extra
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(findProperty("deployVersion")?.toString() ?: buildNumber)

val kotlinLanguageVersion by extra("1.2")

allprojects {
    group = "org.jetbrains.kotlin"
    version = kotlinVersion
}

extra["kotlin_root"] = rootDir

val bootstrapCompileCfg = configurations.create("bootstrapCompile")
val scriptCompileCfg = configurations.create("scriptCompile").extendsFrom(bootstrapCompileCfg)
val scriptRuntimeCfg = configurations.create("scriptRuntime").extendsFrom(scriptCompileCfg)

repositories {
    for (repo in (rootProject.extra["repos"] as List<String>)) {
        maven { setUrl(repo) }
    }
}

dependencies {
    bootstrapCompileCfg(kotlinDep("compiler-embeddable", bootstrapKotlinVersion))
}

val commonBuildDir = File(rootDir, "build")
val distDir = "$rootDir/dist"
val distKotlinHomeDir = "$distDir/kotlinc"
val distLibDir = "$distKotlinHomeDir/lib"
val ideaPluginDir = "$distDir/artifacts/Kotlin"
val ideaUltimatePluginDir = "$distDir/artifacts/KotlinUltimate"

extra["distDir"] = distDir
extra["distKotlinHomeDir"] = distKotlinHomeDir
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["ideaUltimatePluginDir"] = project.file(ideaUltimatePluginDir)
extra["isSonatypeRelease"] = false

Properties().apply {
    load(File(rootDir, "resources", "kotlinManifest.properties").reader())
    forEach {
        val key = it.key
        if (key != null && key is String)
            extra[key] = it.value
    }
}

extra["JDK_16"] = jdkPath("1.6")
extra["JDK_17"] = jdkPath("1.7")
extra["JDK_18"] = jdkPath("1.8")
extra["JDK_9"] = jdkPathIfFound("9")

extra["versions.protobuf-java"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.jansi"] = "1.16"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"
extra["versions.android"] = "2.3.1"

extra["ideaCoreSdkJars"] = arrayOf("annotations", "asm-all", "guava", "intellij-core", "jdom", "jna", "log4j", "picocontainer",
                                   "snappy-in-java", "streamex", "trove4j", "xpp3-1.1.4-min", "xstream")

extra["compilerModules"] = arrayOf(":compiler:util",
                                   ":compiler:container",
                                   ":compiler:conditional-preprocessor",
                                   ":compiler:resolution",
                                   ":compiler:serialization",
                                   ":compiler:frontend",
                                   ":compiler:frontend.java",
                                   ":compiler:frontend.script",
                                   ":compiler:cli-common",
                                   ":compiler:daemon-common",
                                   ":compiler:daemon",
                                   ":compiler:ir.tree",
                                   ":compiler:ir.psi2ir",
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
                                   ":core:util.runtime",
                                   ":core")

val coreLibProjects = listOf(
        ":kotlin-stdlib",
        ":kotlin-stdlib-common",
        ":kotlin-stdlib-js",
        ":kotlin-stdlib-jre7",
        ":kotlin-stdlib-jre8",
        ":kotlin-stdlib-jdk7",
        ":kotlin-stdlib-jdk8",
        ":kotlin-test:kotlin-test-common",
        ":kotlin-test:kotlin-test-jvm",
        ":kotlin-test:kotlin-test-junit",
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

val importedAntTasksPrefix = "imported-ant-update-"

// TODO: check the reasons of import conflict with xerces
//ant.importBuild("$rootDir/update_dependencies.xml") { antTaskName -> importedAntTasksPrefix + antTaskName }

tasks.matching { task ->
    task.name.startsWith(importedAntTasksPrefix)
}.forEach {
    it.group = "Imported ant"
}

//task("update-dependencies") {
//    dependsOn(tasks.getByName(importedAntTasksPrefix + "update"))
//}

fun Project.allprojectsRecursive(body: Project.() -> Unit) {
    this.body()
    this.subprojects { allprojectsRecursive(body) }
}

fun Task.listConfigurationContents(configName: String) {
    doFirst {
        println("$configName configuration files:\n${project.configurations[configName].allArtifacts.files.files.joinToString("\n  ", "  ")}")
    }
}

val defaultJvmTarget = "1.8"
val defaultJavaHome = jdkPath(defaultJvmTarget!!)

allprojects {

    jvmTarget = defaultJvmTarget
    javaHome = defaultJavaHome

    // There are problems with common build dir:
    //  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
    //  - idea seems unable to exclude common builddir from indexing
    // therefore it is disabled by default
//    buildDir = File(commonBuildDir, project.name)

    repositories {
        for (repo in (rootProject.extra["repos"] as List<String>)) {
            maven { setUrl(repo) }
        }
    }
    configureJvmProject(javaHome!!, jvmTarget!!)

    val commonCompilerArgs = listOf("-Xallow-kotlin-package")
    
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            languageVersion = kotlinLanguageVersion
            apiVersion = kotlinLanguageVersion
            freeCompilerArgs = commonCompilerArgs
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
        kotlinOptions {
            freeCompilerArgs = commonCompilerArgs + listOf("-Xnormalize-constructor-calls=enable")
        }
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    task<Jar>("javadocJar") {
        classifier = "javadoc"
    }

    tasks.withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    task("listArchives") { listConfigurationContents("archives") }

    task("listRuntimeJar") { listConfigurationContents("runtimeJar") }

    task("listDistJar") { listConfigurationContents("distJar") }

    afterEvaluate {
        logger.info("configuring project $name to compile to the target jvm version $jvmTarget using jdk: $javaHome")
        if (javaHome != defaultJavaHome || jvmTarget != defaultJvmTarget) {
            configureJvmProject(javaHome!!, jvmTarget!!)
        }
    }
}

task<Copy>("dist") {
    into(distDir)
    from(files("compiler/cli/bin")) { into("kotlinc/bin") }
    from(files("license")) { into("kotlinc/license") }
}

val compilerCopyTask = task<Copy>("idea-plugin-copy-compiler") {
    dependsOnTaskIfExistsRec("dist")
    into(ideaPluginDir)
    from(distDir) { include("kotlinc/**") }
}

task<Copy>("ideaPlugin") {
    dependsOn(compilerCopyTask)
    dependsOnTaskIfExistsRec("idea-plugin")
    shouldRunAfter(":prepare:idea-plugin:idea-plugin")
    into("$ideaPluginDir/lib")
}

task("dist-plugin") {
    dependsOn("ideaPlugin")
    doFirst { logger.warn("'$name' task is deprecated, use '${dependsOn.last()}' instead") }
}

tasks {
    "clean" {
        doLast {
            delete("$buildDir/repo")
            delete(distDir)
        }
    }


    "coreLibsTest" {
        (coreLibProjects + listOf(
                ":kotlin-stdlib:samples",
                ":kotlin-test:kotlin-test-js:kotlin-test-js-it",
                ":tools:binary-compatibility-validator"
        )).forEach {
            dependsOn(it + ":check")
        }
    }

    "gradlePluginTest" {
        gradlePluginProjects.forEach {
            dependsOn(it + ":check")
        }
    }

    "gradlePluginIntegrationTest" {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    "jvmCompilerTest" {
        dependsOn("dist")
        dependsOn(":compiler:test",
                  ":compiler:container:test",
                  ":compiler:tests-java8:test")
    }

    "compiler-tests" {
        dependsOn("jvmCompilerTest")
        doFirst { logger.warn("'$name' task is deprecated, use '${dependsOn.last()}' instead") }
    }

    "jsCompilerTest" {
        dependsOn(":js:js.tests:test")
    }

    "js-tests" {
        dependsOn("jsCompilerTest")
        doFirst { logger.warn("'$name' task is deprecated, use '${dependsOn.last()}' instead") }
    }

    "scriptingTest" {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
    }

    "scripting-tests" {
        dependsOn("scriptingTest")
        doFirst { logger.warn("'$name' task is deprecated, use '${dependsOn.last()}' instead") }
    }

    "compilerTest" {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")

        dependsOn("scriptingTest")
        dependsOn(":kotlin-build-common:test")
    }

    "examplesTest" {
        dependsOn("dist")
        (project(":examples").subprojects + project(":kotlin-gradle-subplugin-example")).forEach { p ->
            dependsOn("${p.path}:check")
        }
    }

    "distTest" {
        dependsOn("compilerTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }


    "jps-tests" {
        dependsOn("dist")
        dependsOn(":jps-plugin:test")
    }

    "idea-plugin-main-tests" {
        dependsOn("dist")
        dependsOn(":idea:test")
    }

    "idea-plugin-additional-tests" {
        dependsOn("dist")
        dependsOn(":idea:idea-gradle:test",
                  ":idea:idea-maven:test",
                  ":j2k:test",
                  ":eval4j:test")
    }

    "idea-plugin-tests" {
        dependsOn("dist")
        dependsOn("idea-plugin-main-tests",
                  "idea-plugin-additional-tests")
    }

    "android-tests" {
        dependsOn("dist")
        dependsOn(":plugins:android-extensions-ide:test",
                  ":idea:idea-android:test",
                  ":kotlin-annotation-processing:test",
                  ":compiler:android-tests:test")
    }

    "plugins-tests" {
        dependsOn("dist")
        dependsOn(":plugins:plugins-tests:test",
                  ":kotlin-annotation-processing:test",
                  ":kotlin-source-sections-compiler-plugin:test",
                  ":plugins:uast-kotlin:test",
                  ":kotlin-annotation-processing-gradle:test")
    }

    "other-tests" {
        dependsOn("dist")
        dependsOn(":kotlin-build-common:test",
                  ":generators:test")
        doFirst { logger.warn("'$name' task is deprecated") }
    }


    "ideaPluginTest" {
        dependsOn(
                "idea-plugin-tests",
                "jps-tests",
                "plugins-tests",
                "android-tests",
                ":generators:test"
        )
    }


    "test" {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }
    "check" { dependsOn("test") }
}

configure<IdeaModel> {
    module {
        excludeDirs = files(
                project.buildDir,
                ".gradle",
                "dependencies",
                "dist",
                "ideaSDK/bin",
                "ideaSDK/androidSDK",
                "ideaSDK/config",
                "ideaSDK/config-idea",
                "ideaSDK/system",
                "ideaSDK/system-idea"
        ).toSet()
    }
}

fun jdkPathIfFound(version: String): String? {
    val jdkName = "JDK_${version.replace(".", "")}"
    val jdkMajorVersion = JdkMajorVersion.valueOf(jdkName)
    return configuredJdks.find { it.majorVersion == jdkMajorVersion }?.homeDir?.canonicalPath
}

fun jdkPath(version: String): String = jdkPathIfFound(version)
        ?: throw GradleException ("Please set environment variable JDK_${version.replace(".", "")} to point to JDK $version installation")

fun Project.configureJvmProject(javaHome: String, javaVersion: String) {

    tasks.withType<JavaCompile> {
        options.isFork = true
        options.forkOptions.javaHome = file(javaHome)
        options.compilerArgs.add("-proc:none")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jdkHome = javaHome
        kotlinOptions.jvmTarget = javaVersion
        doFirst {
            System.setProperty("kotlin.colors.enabled", "false")
        }
    }

    tasks.withType<Test> {
        executable = File(javaHome, "bin/java").canonicalPath
    }
}
