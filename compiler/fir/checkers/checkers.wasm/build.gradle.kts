import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.web.common"))
    implementation(project(":core:compiler.common.wasm"))
    implementation(project(":core:compiler.common.web"))

    // Needed for JS identifier utils
    implementation(project(":js:js.ast"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))

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

tasks.named("compileKotlin").configure {
    dependsOn(":compiler:fir:checkers:generateCheckersComponents")
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(projectDir.resolve("gen"))
    }
}
