@file:Suppress("unused") // usages in build scripts are not tracked properly

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.*

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
        listOf( "com.intellij",
                "com.google",
                "com.sampullara",
                "org.apache",
                "org.jdom",
                "org.picocontainer",
                "jline",
                "gnu",
                "javax.inject",
                "org.fusesource")

fun Project.embeddableCompiler(taskName: String = "embeddable", body: Jar.() -> Unit = {}): Jar {

    val compilerJar = configurations.create("compilerJar")
    dependencies.add(compilerJar.name, dependencies.project(":kotlin-compiler", configuration = "runtimeJar"))

    return task<ShadowJar>(taskName) {
        destinationDir = File(buildDir, "libs")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(compilerJar)
        relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
        packagesToRelocate.forEach {
            relocate(it, "$kotlinEmbeddableRootPackage.$it")
        }
        relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
            // TODO: remove "it." after #KT-12848 get addressed
            exclude("org.fusesource.jansi.internal.CLibrary")
        }
        body()
    }
}

fun Project.rewriteDepsToShadedJar(originalJarTask: Jar, shadowJarTask: Zip, body: Jar.() -> Unit = {}): Jar {
    val originalFiles by lazy {
        val jarContents = zipTree(originalJarTask.outputs.files.singleFile).files
        val basePath = jarContents.find { it.name == "MANIFEST.MF" }?.parentFile?.parentFile ?: throw GradleException("cannot determine the jar root dir")
        jarContents.map { it.relativeTo(basePath).path }.toSet()
    }
    return task<Jar>("rewrittenDepsJar") {
        originalJarTask.apply {
            classifier = "original"
        }
        shadowJarTask.apply {
            dependsOn(originalJarTask)
            from(originalJarTask)
            classifier = "shadow"
        }
        dependsOn(shadowJarTask)
        from(project.zipTree(shadowJarTask.outputs.files.singleFile)) { include { originalFiles.any { originalFile -> it.file.canonicalPath.endsWith(originalFile) } } }
        body()
    }
}

fun Project.rewriteDepsToShadedCompiler(originalJarTask: Jar, body: Jar.() -> Unit = {}): Jar = rewriteDepsToShadedJar(originalJarTask, embeddableCompiler(), body)
