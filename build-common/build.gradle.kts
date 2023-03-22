import org.jetbrains.kotlin.gradle.dsl.KotlinVersion as GradleKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

description = "Kotlin Build Common"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.config"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))

    testCompileOnly(project(":compiler:cli-common"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))
    testApi(protobufFull())
    testApi(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation("org.reflections:reflections:0.10.2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest(parallel = true)

projectTest("testJUnit5", jUnitMode = JUnitMode.JUnit5, parallel = true) {
    useJUnitPlatform()
}

// 1.9 level breaks Kotlin Gradle plugins via changes in enums (KT-48872)
// We limit api and LV until KGP will stop using Kotlin compiler directly (KT-56574)
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.apiVersion.value(GradleKotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    compilerOptions.languageVersion.value(GradleKotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}
