import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    base
    `maven-publish`
}

val builtinsSrc = fileFrom(rootDir, "core", "builtins", "src")
val builtinsNative = fileFrom(rootDir, "core", "builtins", "native")
val kotlinReflectCommon = fileFrom(rootDir, "libraries/stdlib/src/kotlin/reflect/")
val kotlinReflectJvm = fileFrom(rootDir, "libraries/stdlib/jvm/src/kotlin/reflect")
val kotlinRangesCommon = fileFrom(rootDir, "libraries/stdlib/src/kotlin/ranges")
val kotlinCollectionsCommon = fileFrom(rootDir, "libraries/stdlib/src/kotlin/collections")
val kotlinAnnotationsCommon = fileFrom(rootDir, "libraries/stdlib/src/kotlin/annotations")

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

val prepareRangeSources by tasks.registering(Sync::class) {
    from(kotlinRangesCommon) {
        exclude("Ranges.kt")
    }
    from(kotlinCollectionsCommon) {
        include("PrimitiveIterators.kt")
    }
    from(kotlinAnnotationsCommon) {
        include("ExperimentalStdlibApi.kt")
        include("OptIn.kt")
        include("WasExperimental.kt")
    }

    into(layout.buildDirectory.dir("src/ranges"))
}

val prepareSources by tasks.registering(Sync::class) {
    from(kotlinReflectCommon) {
        exclude("typeOf.kt")
        exclude("KClasses.kt")
    }
    into(layout.buildDirectory.dir("src/reflect"))
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
    into(layout.buildDirectory.dir("src-jvm/reflect"))
}

/**
 * @param inDirs a list of input directories. Each value is evaluated as per [Project.file].
 */
fun serializeTask(name: String, sourcesTask: TaskProvider<*>, inDirs: List<Any>) =
    tasks.register(name, NoDebugJavaExec::class) {
        dependsOn(sourcesTask, prepareRangeSources)
        val outDir = layout.buildDirectory.dir(this.name)
        inDirs.forEach { inputs.dir(it).withPathSensitivity(RELATIVE) }
        outputs.dir(outDir)
        outputs.cacheIf { true }

        classpath(rootProject.buildscript.configurations["bootstrapCompilerClasspath"])
        mainClass.set("org.jetbrains.kotlin.serialization.builtins.RunKt")
        jvmArguments.add("-Didea.io.use.nio2=true")

        val inputDirectories = project.files(inDirs)
        argumentProviders.add {
            listOf(
                pathRelativeToWorkingDir(outDir.get().asFile),
                *inputDirectories.map(::pathRelativeToWorkingDir).toTypedArray()
            )
        }
    }

val serialize = serializeTask("serialize", prepareSources, listOf(builtinsSrc, builtinsNative, prepareSources.map { it.destinationDir }, prepareRangeSources.map { it.destinationDir }))

val serializeJvm = serializeTask("serializeJvm", prepareSourcesJvm, listOf(builtinsSrc, builtinsNative, prepareSourcesJvm.map { it.destinationDir }, prepareRangeSources.map { it.destinationDir }))

val builtinsJar by task<Jar> {
    dependsOn(serialize)
    from(serialize) { include("kotlin/**") }
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

val builtinsJvmJar by task<Jar> {
    dependsOn(serializeJvm)
    from(serializeJvm) { include("kotlin/**") }
    archiveClassifier.set("jvm")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
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
        maven(rootProject.layout.buildDirectory.dir("internal/repo"))
    }
}
