plugins {
    id("common-configuration")
    kotlin("jvm")
    application
}

application {
    mainClass.set("research.kapt.gen.MainKt")
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_17_0
    targetBytecodeVersion = JdkMajorVersion.JDK_17_0
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
}
