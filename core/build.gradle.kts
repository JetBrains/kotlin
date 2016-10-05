
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

dependencies {
    compile(project(":core.builtins"))
    compile(project(":libraries:stdlib"))
    compile(project(":custom-dependencies:protobuf-lite", configuration = "protobuf-java")) {
        isTransitive = false
        this
    }
    compile("javax.inject", "javax.inject", "1")
//    compile("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.extra["kotlinVersion"]}")
//    compile("org.jetbrains.kotlin:kotlin-reflect:${rootProject.extra["kotlinVersion"]}")
//    classpath("org.jetbrains.kotlin:kotlin-compiler-embeddable:${rootProject.extra["kotlinVersion"]}")
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        listOf("core/descriptor.loader.java/src",
                "core/descriptors/src",
                "core/descriptors.runtime/src",
                "core/deserialization/src",
                "core/util.runtime/src")
        .map { File(rootDir, it) }
        .let { java.setSrcDirs(it) }
        println(compileClasspath.joinToString("\n    ", prefix = "classpath =\n    ") { it.canonicalFile.relativeTo(rootDir).path })
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.allowKotlinPackage = true
}
