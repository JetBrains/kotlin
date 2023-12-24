import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi"))

    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
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
    "checkers.native",
    "checkers.wasm",
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
    mainClass.set("org.jetbrains.kotlin.fir.checkers.generator.MainKt")
    systemProperties["line.separator"] = "\n"
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs(generateCheckersComponents.map {
            // This is required because that task is generating all the platforms too
            generationRoot
        })
    }
    "test" { none() }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}
