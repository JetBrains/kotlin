plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(project(":compiler:cli:cli-arguments-generator"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:arguments"))
    implementation(project(":compiler:build-tools:util-kotlinpoet"))
    implementation(project(":generators"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
}

application {
    mainClass.set("org.jetbrains.kotlin.buildtools.options.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
}
