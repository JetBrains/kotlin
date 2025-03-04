import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    implementation(project(":core:util.runtime"))

    compileOnly(libs.guava)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
        )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
