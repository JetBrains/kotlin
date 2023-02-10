import org.jetbrains.kotlin.gradle.dsl.KotlinVersion as GradleKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:compiler.version"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:log4j"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(jpsModel()) { isTransitive = false }
    compileOnly(jpsModelImpl()) { isTransitive = false }
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources"))
    }
    "test" {}
}

// 1.9 level breaks Kotlin Gradle plugins via changes in enums (KT-48872)
// We limit api and LV until KGP will stop using Kotlin compiler directly (KT-56574)
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.apiVersion.value(GradleKotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    compilerOptions.languageVersion.value(GradleKotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}
