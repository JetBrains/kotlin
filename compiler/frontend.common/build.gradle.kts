plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:config"))
    implementation(project(":compiler:container"))
    api(project(":core:deserialization.common"))
    api(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


generatedConfigurationKeys("FrontendConfigurationKeys")
