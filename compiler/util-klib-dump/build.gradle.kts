plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:serialization"))
    api(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()
