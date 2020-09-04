import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:resolve"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
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

val generateCheckersComponents by tasks.registering(NoDebugJavaExec::class) {

    val generatorRoot = "$projectDir/checkers-component-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)
    outputs.dirs(generationRoot)

    args(generationRoot)
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
