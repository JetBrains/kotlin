import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("root-config")
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":compiler:resolution.common"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:tree:tree-generator"))
    implementation(project(":compiler:fir:checkers:checkers-component-generator"))
    implementation(project(":analysis:analysis-api"))

    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    /*
     We do not need guava in the generator, but because of a bug in the IJ project importing, we need to have a dependency on intellijCore
     the same as it is in `:fir:tree:tree-generator` module to the project be imported correctly
     */
    compileOnly(intellijCore())
    compileOnly(libs.guava)

    implementation(project(":compiler:psi:psi-api"))
}

application {
    mainClass.set("org.jetbrains.kotlin.analysis.api.fir.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
            "org.jetbrains.kotlin.analysis.api.KaIdeApi",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction",
            "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
            "org.jetbrains.kotlin.analysis.api.components.KaSessionComponentImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
        )
    )
}
