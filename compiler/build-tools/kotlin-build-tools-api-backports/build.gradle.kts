plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("generated-sources")
    id("test-inputs-check")
}

dependencies {
    compileOnly(kotlinStdlib())
    compileOnly(project(":compiler:cli"))
}

class StripMetadataAction : Action<Task> {
    override fun execute(task: Task) {
        with(task) {
            val inFile = outputs.files.singleFile
            val outFile = inFile.resolveSibling(inFile.name + "-stripped.jar")
            stripMetadata(
                logger = logger,
                classNamePattern = ".*",
                inFile = inFile,
                outFile = outFile,
                preserveFileTimestamps = true
            )
            check(outFile.renameTo(inFile))
        }
    }
}

// Gradle will freak out when it sees newer Kotlin version metadata on its classpath, so we need to remove kotlin_module and @Metadata annotations
// The classes from here will only be accessed using Java reflection anyway, and are not exposed to Kotlin users.

tasks.jar {
    exclude("**/*.kotlin_module")
}

abstract class StripKotlinMetadata : DefaultTask() {

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun process() {
        val inputFile = inputJar.get().asFile
        val outputFile = outputJar.get().asFile

        stripMetadata(
            logger = logger,
            classNamePattern = ".*",
            inFile = inputFile,
            outFile = outputFile,
            preserveFileTimestamps = true
        )
    }
}

val stripMetadataTask = tasks.register<StripKotlinMetadata>("stripMetadata") {
    // Wire the default 'jar' task's archive file as the input
    inputJar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })

    // Set the output file location in the build directory
    outputJar.set(
        layout.buildDirectory.file("libs/${project.name}-${project.version}-stripped.jar")
    )
}

configurations.named("runtimeElements") {
    outgoing.artifacts.clear()
    outgoing.artifact(stripMetadataTask)
}

configurations.named("apiElements") {
    outgoing.artifacts.clear()
    outgoing.artifact(stripMetadataTask)
}
