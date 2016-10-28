import java.util.*
import java.io.File

extra["kotlinVersion"] = file("kotlin-version-for-gradle.txt").readText().trim()
extra["repo"] = "https://repo.gradle.org/gradle/repo"
extra["build.number"] = "1.1-SNAPSHOT"
extra["distDir"] = "$rootDir/build/dist"
Properties().apply {
    load(File("resources/kotlinManifest.properties").reader())
    forEach {
        val key = it.key
        if (key != null && key is String)
            extra[key] = it.value
    }
}

extra["versions.protobuf-java"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.cli-parser"] = "1.1.2"
extra["versions.jansi"] = "1.11"
extra["versions.jline"] = "2.12.1"

//buildscript {
//    //extra["kotlinVersion"] = "1.1-M01"
//    extra["kotlinVersion"] = file("kotlin-version-for-gradle.txt").readText().trim()
//    extra["repo"] = "https://repo.gradle.org/gradle/repo"
//
//    repositories {
//        mavenLocal()
//        maven { setUrl(extra["repo"]) }
//    }
//
//    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
//        classpath("org.jetbrains.kotlin:kotlin-compiler-embeddable:${extra["kotlinVersion"]}")
//    }
//}
//
//apply { plugin("kotlin") }

val importedAntTasksPrefix = "imported-ant-update-"

ant.importBuild("update_dependencies.xml") { antTaskName -> importedAntTasksPrefix + antTaskName }

tasks.matching { task ->
    task.name.startsWith(importedAntTasksPrefix)
}.forEach {
    it.group = "Imported ant"
}

task("update-dependencies") {
    dependsOn(tasks.getByName(importedAntTasksPrefix + "update"))
}

allprojects {
    setBuildDir("$rootDir/build/${project.name}")
}
