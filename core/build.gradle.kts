
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

apply {
    plugin("kotlin")
}

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

dependencies {
//    compile("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.extra["kotlinVersion"]}")
//    compile("org.jetbrains.kotlin:kotlin-reflect:${rootProject.extra["kotlinVersion"]}")
    compile(project(":core:builtins"))
    compile(project(":libraries:stdlib"))
//    compile(project(":custom-dependencies:protobuf-lite"))
    compile("javax.inject", "javax.inject", "1")
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
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

task("sourcesets") {
    doLast {
        the<JavaPluginConvention>().sourceSets.all {
            println("--> ${it.name}: ${it.java.srcDirs.joinToString()}")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.moduleName = "kotlin-core"
    kotlinOptions.allowKotlinPackage = true
}
