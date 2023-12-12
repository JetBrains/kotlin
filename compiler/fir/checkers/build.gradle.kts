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
        java.srcDirs(generateCheckersComponents)
    }
    "test" { none() }
}
