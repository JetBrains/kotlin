plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:cli-base"))
    implementation(project(":compiler:javac-wrapper"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:frontend.common.jvm"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()
