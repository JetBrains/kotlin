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

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
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
