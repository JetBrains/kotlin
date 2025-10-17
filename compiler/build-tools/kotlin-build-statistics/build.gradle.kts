description = "Kotlin Build Report Common"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-published-compiler-dependency-configuration")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":kotlin-util-io"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(commonDependency("com.google.code.gson:gson"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()
