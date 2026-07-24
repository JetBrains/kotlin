plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

optInToK1Deprecation()

optInToUnsafeDuringIrConstructionAPI()
