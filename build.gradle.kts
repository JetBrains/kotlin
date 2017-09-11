import java.util.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.java
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

buildscript {
    extra["kotlin_version"] = file("kotlin-bootstrap-version.txt").readText().trim()
    extra["kotlinVersion"] = extra["kotlin_version"]
    extra["kotlin_language_version"] = "1.1"
    extra["kotlin_gradle_plugin_version"] = extra["kotlin_version"]
    extra["repos"] = listOf("https://dl.bintray.com/kotlin/kotlin-dev",
                            "https://repo.gradle.org/gradle/repo",
                            "https://plugins.gradle.org/m2",
                            "http://repository.jetbrains.com/utils/")

    repositories {
        for (repo in (rootProject.extra["repos"] as List<String>)) {
            maven { setUrl(repo) }
        }
    }

    dependencies {
        classpath(kotlinDep("gradle-plugin"))
        classpath("com.gradle.publish:plugin-publish-plugin:0.9.7")
    }
}

plugins {
    java // so we can benefit from the `java()` accessor below
}

val buildNumber = "1.1-SNAPSHOT"
extra["build.number"] = buildNumber

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
    bootstrapCompileCfg(kotlinDep("compiler-embeddable"))
}

val commonBuildDir = File(rootDir, "build")
val distDir = "$rootDir/dist"
val distLibDir = "$distDir/kotlinc/lib"
val ideaPluginDir = "$distDir/artifacts/Kotlin"

extra["distDir"] = distDir
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)

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

extra["compilerBaseName"] = "kotlin-compiler"
extra["embeddableCompilerBaseName"] = "kotlin-compiler-embeddable"
//extra["compilerJarWithBootstrapRuntime"] = project.file("$distDir/kotlin-compiler-with-bootstrap-runtime.jar")
//extra["bootstrapCompilerFile"] = bootstrapCfg.files.first().canonicalPath

extra["buildLocalRepoPath"] = File(commonBuildDir, "repo")

extra["versions.protobuf-java"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.cli-parser"] = "1.1.2"
extra["versions.jansi"] = "1.11"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"

extra["ideaCoreSdkJars"] = arrayOf("annotations", "asm-all", "guava", "intellij-core", "jdom", "jna", "log4j", "picocontainer",
                                   "snappy-in-java", "trove4j", "xpp3-1.1.4-min", "xstream")

extra["compilerModules"] = arrayOf(":compiler:util",
                                   ":compiler:container",
                                   ":compiler:resolution",
                                   ":compiler:serialization",
                                   ":compiler:frontend",
                                   ":compiler:frontend.java",
                                   ":compiler:frontend.script",
                                   ":compiler:cli-common",
                                   ":compiler:daemon-common",
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
                                   ":build-common",
                                   ":core:util.runtime",
                                   ":core")

allprojects {
    group = "org.jetbrains.kotlin"
    version = buildNumber
}

apply {
    from("libraries/commonConfiguration.gradle")
    from("libraries/gradlePluginsConfiguration.gradle")
    from("libraries/configureGradleTools.gradle")
//    from("libraries/prepareSonatypeStaging.gradle")
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

//val prepareBootstrapTask = task("prepareBootstrap") {
//    dependsOn(bootstrapCfg, scriptCompileCfg, scriptRuntimeCfg)
//}

fun Project.allprojectsRecursive(body: Project.() -> Unit) {
    this.body()
    this.subprojects { allprojectsRecursive(body) }
}

allprojects {

    setBuildDir(File(commonBuildDir, project.name))

    repositories {
        for (repo in (rootProject.extra["repos"] as List<String>)) {
            maven { setUrl(repo) }
        }
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", project.name)
    }

    tasks.withType<Kotlin2JsCompile> {
        kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", project.name)
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    task<Jar>("javadocJar") {
        classifier = "javadoc"
    }
}

task<Copy>("dist") {
    into(distDir)
    from(files("compiler/cli/bin")) { into("kotlinc/bin") }
}

val compilerCopyTask = task<Copy>("idea-plugin-copy-compiler") {
    dependsOnTaskIfExistsRec("dist")
    into(ideaPluginDir)
    from(distDir) { include("kotlinc/**") }
}

task<Copy>("dist-plugin") {
    dependsOn(compilerCopyTask)
    dependsOnTaskIfExistsRec("idea-plugin")
    into("$ideaPluginDir/lib")
}

val clean by tasks
clean.apply {
    doLast {
        delete("${buildDir}/repo")
    }
}

fun jdkPath(version: String): String {
    val varName = "JDK_${version.replace(".", "")}"
    return System.getenv(varName) ?: throw GradleException ("Please set environment variable $varName to point to JDK $version installation")
}
