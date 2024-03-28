import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:util"))
    compileOnly(intellijCore())
    implementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project(":compiler:bir.tree:tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }
}

optInToUnsafeDuringIrConstructionAPI()

tasks.compileKotlin {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" {}
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("tree-generator"))
}

val generationRoot = projectDir.resolve("gen")

val generateTree by tasks.registering(NoDebugJavaExec::class) {

    val generatorRoot = "$projectDir/tree-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)
    outputs.dirs(generationRoot)

    args(generationRoot)
    workingDir = rootDir
    classpath = generatorClasspath
    mainClass.set("org.jetbrains.kotlin.bir.generator.MainKt")
    systemProperties["line.separator"] = "\n"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateTree)

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}
