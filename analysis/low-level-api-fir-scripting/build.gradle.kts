plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:low-level-api-fir"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:fir:plugin.api"))
    implementation(project(":kotlin-assignment-compiler-plugin.cli"))
    implementation(project(":kotlin-assignment-compiler-plugin.k2"))
    implementation(project(":kotlin-scripting-common"))
    implementation(project(":kotlin-scripting-compiler"))
    implementation(project(":kotlin-scripting-jvm"))

    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    compilerOptions.optIn.addAll(
        "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
        "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
        "org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals",
    )
}
