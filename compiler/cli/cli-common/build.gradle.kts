plugins {
    kotlin("jvm")
    id("jps-compatible")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":core:util.runtime"))
    api(project(":compiler:config"))
    api(project(":compiler:config.jvm"))
    api(project(":js:js.config"))
    api(project(":wasm:wasm.config"))
    api(project(":native:kotlin-native-utils"))
    api(project(":compiler:plugin-api"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    implementation(libs.kotlinx.serialization.json)
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" {}
}

optInToExperimentalCompilerApi()

tasks.getByName<Jar>("jar") {
    //excludes unused bunch files
    exclude("META-INF/extensions/*.xml.**")
}
