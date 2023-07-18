import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common.js"))
    api(project(":js:js.ast"))
    api(project(":compiler:fir:checkers"))

    // FE checks for modules use ModuleKind
    // This dependency can be removed when we stop supporting PLAIN and UMD module systems
    implementation(project(":js:js.serializer"))

    /*
     * We can't remove this dependency until we use
     *   diagnostics framework from FE 1.0
     */
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:psi"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
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
