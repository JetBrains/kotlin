plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    implementation(project(":core:util.runtime"))
    implementation(project(":compiler:config"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":js:js.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":compiler:plugin-api"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.asm)
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
