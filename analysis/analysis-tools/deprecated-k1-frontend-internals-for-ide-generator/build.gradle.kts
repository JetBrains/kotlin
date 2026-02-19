plugins {
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
