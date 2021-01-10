
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    runtimeOnly(intellijDep())
    runtimeOnly(intellijRuntimeAnnotations())
    runtimeOnly(toolsJar())
}

val ideaSandboxDir: File by rootProject.extra

runIdeTask("runIde", rootDir.resolve("out/artifacts/KotlinPluginCommunity"), ideaSandboxDir) {
    dependsOn(":jarsForIde")
}
