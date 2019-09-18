import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val COMPOSE_VERSION = "1.3.50-compose-20190806"
val KOTLIN_COMPOSE_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:$COMPOSE_VERSION"

repositories {
    maven(project.rootProject.projectDir.resolve("../../prebuilts/androidx/external").absolutePath)
}

dependencies {
    compileOnly(KOTLIN_COMPOSE_STDLIB)
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$COMPOSE_VERSION")
    compileOnly("org.jetbrains.kotlin:kotlin-plugin:$COMPOSE_VERSION")
    compileOnly("org.jetbrains.kotlin:kotlin-intellij-core:$COMPOSE_VERSION")
    compileOnly("org.jetbrains.kotlin:kotlin-platform-api:$COMPOSE_VERSION")
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

sourceSets.maybeCreate("main").java.srcDirs("src")