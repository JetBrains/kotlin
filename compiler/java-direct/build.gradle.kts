plugins {
    kotlin("jvm")
    id("test-inputs-check")
}

repositories {
    mavenCentral()
    maven { setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

dependencies {
    api(project(":compiler:frontend.java"))

    implementation(libs.org.jetbrains.syntax.api)
    implementation(libs.org.jetbrains.java.syntax.jvm)

    testApi(kotlinTest("junit"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
