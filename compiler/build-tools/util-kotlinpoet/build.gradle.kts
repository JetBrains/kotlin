plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib("jdk8"))
    api(libs.kotlinpoet) { isTransitive = false }
    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
