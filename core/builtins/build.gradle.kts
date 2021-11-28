import org.gradle.api.tasks.PathSensitivity.RELATIVE
import java.io.File

plugins {
    base
    `maven-publish`
}

val builtinsSrc = fileFrom(rootDir, "core", "builtins", "src")
val builtinsNative = fileFrom(rootDir, "core", "builtins", "native")
val kotlinReflectCommon = fileFrom(rootDir, "libraries/stdlib/src/kotlin/reflect/")
val kotlinReflectJvm = fileFrom(rootDir, "libraries/stdlib/jvm/src/kotlin/reflect")
val builtinsCherryPicked = fileFrom(buildDir, "src")
val builtinsCherryPickedJvm = fileFrom(buildDir, "src-jvm")

val runtimeElements by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val runtimeElementsJvm by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Attribute.of("builtins.platform", String::class.java), "JVM")
    }
}

val prepareSources by tasks.registering(Sync::class) {
    from(kotlinReflectCommon) {
        exclude("typeOf.kt")
        exclude("KClasses.kt")
    }
    into(builtinsCherryPicked)
}

val prepareSourcesJvm by tasks.registering(Sync::class) {
    from(kotlinReflectJvm) {
        exclude("TypesJVM.kt")
        exclude("KClassesImpl.kt")
    }
    from(kotlinReflectCommon) {
        include("KTypeProjection.kt")
        include("KClassifier.kt")
        include("KTypeParameter.kt")
        include("KVariance.kt")
    }
    into(builtinsCherryPickedJvm)
}

fun serializeTask(name: String, sourcesTask: TaskProvider<*>, inDirs: List<File>) =
    tasks.register(name, NoDebugJavaExec::class) {
        dependsOn(sourcesTask)
        val outDir = buildDir.resolve(this.name)
        inDirs.forEach { inputs.dir(it).withPathSensitivity(RELATIVE) }
        outputs.dir(outDir)
        outputs.cacheIf { true }

        classpath(rootProject.buildscript.configurations["bootstrapCompilerClasspath"])
        main = "org.jetbrains.kotlin.serialization.builtins.RunKt"
        jvmArgs(listOfNotNull(
            "-Didea.io.use.nio2=true",
            "-Dkotlin.builtins.serializer.log=true".takeIf { logger.isInfoEnabled }
        ))
        args(
            pathRelativeToWorkingDir(outDir),
            *inDirs.map(::pathRelativeToWorkingDir).toTypedArray()
        )
    }

val serialize = serializeTask("serialize", prepareSources, listOf(builtinsSrc, builtinsNative, builtinsCherryPicked))

val serializeJvm = serializeTask("serializeJvm", prepareSourcesJvm, listOf(builtinsSrc, builtinsNative, builtinsCherryPickedJvm))

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(serialize) { include("kotlin/**") }
    destinationDir = File(buildDir, "libs")
}

val builtinsJvmJar by task<Jar> {
    dependsOn(serializeJvm)
    from(serializeJvm) { include("kotlin/**") }
    archiveClassifier.set("jvm")
    destinationDir = File(buildDir, "libs")
}

val assemble by tasks.getting {
    dependsOn(serialize)
    dependsOn(serializeJvm)
}

val builtinsJarArtifact = artifacts.add(runtimeElements.name, builtinsJar)
artifacts.add(runtimeElementsJvm.name, builtinsJvmJar)

publishing {
    publications {
        create<MavenPublication>("internal") {
            artifact(builtinsJarArtifact)
        }
    }

    repositories {
        maven("${rootProject.buildDir}/internal/repo")
    }
}
