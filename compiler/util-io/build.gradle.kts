plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
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
