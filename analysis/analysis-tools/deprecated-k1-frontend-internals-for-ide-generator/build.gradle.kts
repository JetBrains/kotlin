plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    application
    kotlin("jvm")
}

dependencies {
    implementation(kotlinStdlib())
}

application {
    mainClass.set("org.jetbrains.kotlin.tools.deprecated.k1.frontend.internals.forIde.generator.MainKt")
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

tasks.withType<JavaExec> {
    workingDir = rootProject.projectDir
}
