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
    implementation(project(":analysis:analysis-internal-utils"))
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
