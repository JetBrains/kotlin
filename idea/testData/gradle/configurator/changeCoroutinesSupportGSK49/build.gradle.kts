import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm") version "1.2.50"
}

kotlin {
    experimental.coroutines = Coroutines.WARN
}
