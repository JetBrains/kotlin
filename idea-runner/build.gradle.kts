
apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))

    compile(intellijDep())
    
    runtimeOnly(files(toolsJar()))
}

val ideaPluginDir: File by rootProject.extra
val ideaSandboxDir: File by rootProject.extra

runIdeTask("runIde", ideaPluginDir, ideaSandboxDir) {

    dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":ideaPlugin")
}
