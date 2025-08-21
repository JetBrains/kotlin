plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    testImplementation(libs.junit4)
    testImplementation(kotlin("test"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()
