@file:Suppress("unused") // usages in build scripts are not tracked properly

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import java.io.File

const val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
    listOf(
        "com.intellij",
        "com.google",
        "com.sampullara",
        "org.apache",
        "org.jdom",
        "org.picocontainer",
        "org.jline",
        "org.fusesource",
        "net.jpountz",
        "one.util.streamex",
        "it.unimi.dsi.fastutil",
        "kotlinx.collections.immutable"
    )

// The shaded compiler "dummy" is used to rewrite dependencies in projects that are used with the embeddable compiler
// on the runtime and use some shaded dependencies from the compiler
// To speed-up rewriting process we want to have this dummy as small as possible.
// But due to the shadow plugin bug (https://github.com/johnrengelman/shadow/issues/262) it is not possible to use
// packagesToRelocate list to for the include list. Therefore the exclude list has to be created.
val packagesToExcludeFromDummy =
    listOf(
        "org/jetbrains/kotlin/**",
        "org/intellij/lang/annotations/**",
        "org/jetbrains/jps/**",
        "META-INF/**",
        "com/sun/jna/**",
        "com/thoughtworks/xstream/**",
        "javaslang/**",
        "*.proto",
        "messages/**",
        "net/sf/cglib/**",
        "one/util/streamex/**",
        "org/iq80/snappy/**",
        "org/jline/**",
        "org/xmlpull/**",
        "*.txt"
    )

private fun ShadowJar.configureEmbeddableCompilerRelocation(withJavaxInject: Boolean = true) {
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    if (withJavaxInject) {
        relocate("javax.inject", "$kotlinEmbeddableRootPackage.javax.inject")
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}

private fun Project.compilerShadowJar(taskName: String, body: ShadowJar.() -> Unit): TaskProvider<out ShadowJar> {

    val compilerJar = configurations.getOrCreate("compilerJar")
    dependencies.add(compilerJar.name, dependencies.project(":kotlin-compiler", configuration = "runtimeJar"))

    return tasks.register<ShadowJar>(taskName) {
        destinationDirectory.set(project.file(File(buildDir, "libs")))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(compilerJar)
        body()
    }
}

fun Project.embeddableCompiler(taskName: String = "embeddable", body: ShadowJar.() -> Unit = {}): TaskProvider<out ShadowJar> =
    compilerShadowJar(taskName) {
        configureEmbeddableCompilerRelocation()
        body()
    }

fun Project.compilerDummyForDependenciesRewriting(
    taskName: String = "compilerDummy", body: ShadowJar.() -> Unit = {}
): TaskProvider<out Jar> =
    compilerShadowJar(taskName) {
        exclude(packagesToExcludeFromDummy)
        body()
    }

const val COMPILER_DUMMY_JAR_CONFIGURATION_NAME = "compilerDummyJar"

fun Project.compilerDummyJar(task: TaskProvider<out Jar>, body: Jar.() -> Unit = {}) {
    task.configure(body)
    task.configure {
        addArtifact(COMPILER_DUMMY_JAR_CONFIGURATION_NAME, this, this)
    }
}

fun Project.embeddableCompilerDummyForDependenciesRewriting(
    taskName: String = "embeddable", body: Jar.() -> Unit = {}
): TaskProvider<ShadowJar> {
    val compilerDummyJar = configurations.getOrCreate("compilerDummyJar")
    dependencies.add(
        compilerDummyJar.name,
        dependencies.project(":kotlin-compiler-embeddable", configuration = COMPILER_DUMMY_JAR_CONFIGURATION_NAME)
    )

    return tasks.register<ShadowJar>(taskName) {
        destinationDirectory.set(project.file(File(buildDir, "libs")))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(compilerDummyJar)
        configureEmbeddableCompilerRelocation(withJavaxInject = false)
        body()
    }
}

fun Project.rewriteDepsToShadedJar(
    originalJarTask: TaskProvider<out Jar>, shadowJarTask: TaskProvider<out Jar>, body: Jar.() -> Unit = {}
): TaskProvider<out Jar> {
    originalJarTask.configure {
        archiveClassifier.set("original")
    }


    shadowJarTask.configure {
        dependsOn(originalJarTask)
        from(originalJarTask)// { include("**") }

        // When Gradle traverses the inputs, reject the shaded compiler JAR,
        // which leads to the content of that JAR being excluded as well:
        val compilerDummyJarFile = project.provider { configurations.getByName("compilerDummyJar").singleFile }
        exclude { it.file == compilerDummyJarFile.get() }

        archiveClassifier.set("original")
        body()
    }
    return shadowJarTask
}

fun Project.rewriteDepsToShadedCompiler(originalJarTask: TaskProvider<out Jar>, body: Jar.() -> Unit = {}): TaskProvider<out Jar> =
    rewriteDepsToShadedJar(originalJarTask, embeddableCompilerDummyForDependenciesRewriting(), body)

fun Project.rewriteDefaultJarDepsToShadedCompiler(body: Jar.() -> Unit = {}): TaskProvider<out Jar> =
    rewriteDepsToShadedJar(tasks.named<Jar>("jar"), embeddableCompilerDummyForDependenciesRewriting(), body)
