plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:resolution"))
    api(project(":compiler:util"))
    api(project(":compiler:config.jvm"))
    compileOnly("javax.annotation:jsr250-api:1.0")
    implementation(project(":compiler:frontend"))
    api(project(":compiler:resolution.common.jvm"))
    api(project(":compiler:frontend.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()
