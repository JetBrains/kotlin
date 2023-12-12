import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:checkers"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:fir:native"))

    compileOnly(project(":core:compiler.common.native"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
}

val generatorClasspath: Configuration by configurations.creating
dependencies {
    generatorClasspath(project(":compiler:fir:checkers:checkers-component-generator"))
}
val generateCheckersComponents by tasks.registering(NoDebugJavaExec::class) {
    workingDir = rootDir
    classpath = generatorClasspath
    mainClass.set("org.jetbrains.kotlin.fir.checkers.generator.MainKt")
    systemProperties["line.separator"] = "\n"

    val generationRoot = layout.projectDirectory.dir("gen")
    args(project.name, generationRoot)
    outputs.dir(generationRoot)
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir(generateCheckersComponents)
    }
    "test" { none() }
}

val compileKotlin by tasks
compileKotlin.dependsOn(":compiler:fir:checkers:generateCheckersComponents")

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(projectDir.resolve("gen"))
    }
}
