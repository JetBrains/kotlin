plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":compiler:container"))
    api(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

generatedConfigurationKeys("FrontendConfigurationKeys")
