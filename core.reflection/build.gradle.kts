
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.org.objectweb.asm.*
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(files("$rootDir/ideaSDK/lib/asm-all.jar"))
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
}

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", implementationTitle)
        put("Implementation-Version", rootProject.extra["build.number"])
    }
    from(configurations.getByName("build-version").files) {
        into("META-INF/")
    }
}

fun DependencyHandler.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return add(cfg.name, project(":prepare:build.version", configuration = "default"))
}

fun commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${rootProject.extra["versions.$coord"]}"
        2 -> "${parts[0]}:${parts[1]}:${rootProject.extra["versions.${parts[1]}"]}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun commonDep(group: String, artifact: String): String = "$group:$artifact:${rootProject.extra["versions.$artifact"]}"

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun KotlinDependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"

fun Project.fixKotlinTaskDependencies() {
    the<JavaPluginConvention>().sourceSets.all { sourceset ->
        val taskName = if (sourceset.name == "main") "classes" else (sourceset.name + "Classes")
        tasks.withType<Task> {
            if (name == taskName) {
                dependsOn("copy${sourceset.name.capitalize()}KotlinClasses")
            }
        }
    }
}

// TODO: common ^ 8< ----

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main")?.apply {
        val srcs = listOf(File(rootDir, "core/reflection.jvm/src"))
        java.setSrcDirs(srcs)
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

dependencies {
    compile(project(":core.builtins"))
    compile(project(":core"))
    compile(project(":libraries:stdlib"))
    compile(protobufLite())
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-reflection")
}

fixKotlinTaskDependencies()

