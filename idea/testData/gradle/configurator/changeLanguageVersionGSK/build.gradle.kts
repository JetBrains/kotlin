import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   kotlin("jvm") version "{{kotlin_plugin_version}}"
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
   languageVersion = "1.0"
}
