import com.sun.javafx.scene.CameraHelper.project
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon : List<Pair<String, String>> by rootProject.extra

dependencies {
    compile(project(":compiler:cli"))
    compile(project(":compiler:cli-js"))
    compile(project(":compiler:daemon-common"))
    compile(project(":compiler:daemon-common-new"))
    compile(project(":compiler:incremental-compilation-impl"))
    compile(project(":kotlin-build-common"))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    runtime(project(":kotlin-reflect"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compile(commonDep("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
kotlin {
    experimental.coroutines = Coroutines.ENABLE
}