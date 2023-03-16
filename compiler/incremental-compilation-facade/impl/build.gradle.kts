plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":incremental-compilation-facade-api"))
    implementation(kotlinStdlib())
    implementation(project(":kotlin-compiler-embeddable"))
    implementation(project(":kotlin-daemon-client"))
    implementation(project(":kotlin-daemon-embeddable"))
    implementation(project(":kotlin-compiler-runner"))
    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project(":kotlin-compiler-runner-unshaded"))
        compileOnly(project(":kotlin-build-common"))
        compileOnly(project(":daemon-common"))
        compileOnly(project(":kotlin-daemon-client"))
        compileOnly(project(":compiler:incremental-compilation-impl"))
    }
}

publish()