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
    implementation(project(":compiler:psi:parser"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(libs.caffeine)
    api(project(":compiler:cli-base"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:light-classes-base"))
    api(project(":analysis:analysis-api-platform-interface"))
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
