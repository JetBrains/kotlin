
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))

    runtimeOnly(intellijDep())
    runtimeOnly(intellijRuntimeAnnotations())
    runtimeOnly(toolsJar())
}

val ideaPluginDir: File by rootProject.extra
val ideaSandboxDir: File by rootProject.extra

runIdeTask("runIde", ideaPluginDir, ideaSandboxDir) {
    dependsOn(":dist", ":ideaPlugin")
}
