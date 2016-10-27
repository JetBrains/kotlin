
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(configurations.getByName("build-version"))
    evaluationDependsOn(":prepare:build.version")
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
    return add(cfg.name, project(":prepare:build.version", configuration = "prepared-build-version"))
}

// TODO: move most of the code above to the root or utility script

dependencies {
    compile(project(":core.builtins"))
    buildVersion()
}

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(File(rootDir, "core/script.runtime/src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package")
}

tasks.withType<Jar> {
    setupRuntimeJar("Kotlin Script Runtime")
    archiveName = "kotlin-script-runtime.jar"
}

