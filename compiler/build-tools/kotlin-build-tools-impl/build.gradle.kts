plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
    implementation(project(":kotlin-compiler-embeddable"))
    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project(":compiler:cli"))
        compileOnly(project(":compiler:cli-js"))
    }
}

publish()

standardPublicJars()