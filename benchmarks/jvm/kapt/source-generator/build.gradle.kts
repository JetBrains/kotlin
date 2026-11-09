plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("research.kapt.gen.MainKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
