
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

fun KotlinDependencyHandler.protobufLite() = project(":custom-dependencies:protobuf-lite", configuration = "default").apply { isTransitive = false }
fun protobufLiteTask() = ":custom-dependencies:protobuf-lite:prepare"

// TODO: common ^ 8< ----

dependencies {
    compile(protobufLite())
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(File(rootDir, "core/builtins/src"), File(rootDir, "core/runtime.jvm/src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

//task("sourcesets") {
//    doLast {
//        the<JavaPluginConvention>().sourceSets.all {
//            println("--> ${it.name}: ${it.java.srcDirs.joinToString()}")
//        }
//    }
//}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask())
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask())
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "moduleName", "kotlin-builtins")
}

