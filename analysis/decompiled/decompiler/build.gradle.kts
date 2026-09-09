plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))

    api(intellijCore())
}

kotlin {
    // The decompiler builds Kotlin PSI and stubs for binary files, so it opts in to all of
    // psi-api's non-public markers (the same list psi-api excludes from its ABI dump)
    // instead of repeating the suppression in every file.
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.psi.KtImplementationDetail",
            "org.jetbrains.kotlin.psi.KtNonPublicApi",
            "org.jetbrains.kotlin.psi.KtIdeApi",
            "org.jetbrains.kotlin.psi.KtExperimentalApi",
            "org.jetbrains.kotlin.psi.KtPlatformInterface",
        )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
