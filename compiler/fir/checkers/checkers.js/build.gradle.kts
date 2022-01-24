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

val compileKotlin by tasks
compileKotlin.dependsOn(":compiler:fir:checkers:generateCheckersComponents")

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(projectDir.resolve("gen"))
    }
}
