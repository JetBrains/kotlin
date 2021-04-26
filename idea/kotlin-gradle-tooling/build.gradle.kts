description = "Kotlin Gradle Tooling support"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())

    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijDep()) { includeJars("slf4j-api-1.7.25") }

    testImplementation(commonDep("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
    testCompileOnly(projectTests(":idea:idea-test-framework"))
    testCompileOnly(intellijDep())

}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xsuppress-deprecated-jvm-target-warning"
    }
}

runtimeJar()

sourcesJar()

javadocJar()
