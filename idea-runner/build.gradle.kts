import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunIdeTask

buildscript {
    repositories {
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") // for intellij plugin
        maven(url = "http://dl.bintray.com/jetbrains/intellij-plugin-service") // for intellij plugin
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.3.0-SNAPSHOT")
    }
}

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))
    compileOnly(intellijDep())

    runtimeOnly(files(toolsJar()))
}

afterEvaluate {
    val ideaPluginDir: File by rootProject.extra
    val ideaSandboxDir: File by rootProject.extra

    tasks.findByName("runIde")?.let { tasks.remove(it) }
    tasks.findByName("prepareSandbox")?.let { tasks.remove(it) }

    val prepareSandbox by task<PrepareSandboxTask> {
        configDirectory = File(ideaSandboxDir, "config")
        // the rest are just some "fake" values, to keeping PrepareSandboxTask happy
        setPluginName("Kotlin")
        dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":ideaPlugin")
        setPluginJar(File(ideaPluginDir, "lib/kotlin-script-runtime.jar")) // any small jar will do
        destinationDir = File(buildDir, "sandbox-fake")
    }

    task<RunIdeTask>("runIde") {
        dependsOn(prepareSandbox)
        group = "intellij"
        description = "Runs Intellij IDEA with installed plugin."
        setIdeaDirectory(intellijRootDir())
        setConfigDirectory(File(ideaSandboxDir, "config"))
        setSystemDirectory(ideaSandboxDir)
        setPluginsDirectory(ideaPluginDir.parent)
        jvmArgs(
                "-Xmx1250m",
                "-XX:ReservedCodeCacheSize=240m",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-ea",
                "-Didea.is.internal=true",
                "-Didea.debug.mode=true",
                "-Dapple.laf.useScreenMenuBar=true",
                "-Dapple.awt.graphics.UseQuartz=true",
                "-Dsun.io.useCanonCaches=false",
                "-Dkotlin.internal.mode.enabled=true",
                "-Didea.ProcessCanceledException=disabled"
        )
    }
}
