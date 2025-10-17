plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(libs.junit4)
    testImplementation(kotlin("test", coreDepsVersion))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()
