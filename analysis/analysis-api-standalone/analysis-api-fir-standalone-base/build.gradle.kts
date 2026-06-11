plugins {
    kotlin("jvm")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-api-fir"))
    implementation(project(":analysis:symbol-light-classes"))
    implementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:decompiled:decompiler-native"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":compiler:frontend.common.jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }
}
