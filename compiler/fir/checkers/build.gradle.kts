import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
        this.java.srcDir("gen")
    }
    "test" { none() }
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("checkers-component-generator"))
}

val generationRoot = projectDir.resolve("gen")

// Add modules for js and native checkers here
val platformGenerationRoots = listOf(
    "checkers.jvm",
    "checkers.js",
).map { projectDir.resolve(it).resolve("gen") }

val generateCheckersComponents by tasks.registering(NoDebugJavaExec::class) {

    val generatorRoot = "$projectDir/checkers-component-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)
    outputs.dirs(generationRoot, *platformGenerationRoots.toTypedArray())

    args(generationRoot, *platformGenerationRoots.toTypedArray())
    workingDir = rootDir
    classpath = generatorClasspath
    main = "org.jetbrains.kotlin.fir.checkers.generator.MainKt"
    systemProperties["line.separator"] = "\n"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateCheckersComponents)

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}
