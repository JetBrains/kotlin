plugins {
    kotlin("jvm")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:decompiled:decompiler-native"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(libs.caffeine)
    implementation(project(":compiler:cli-base"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":analysis:analysis-api-platform-interface"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaExperimentalApi")
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }
}

optInToK1Deprecation()
