
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

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "moduleName", "kotlin-builtins")
}

