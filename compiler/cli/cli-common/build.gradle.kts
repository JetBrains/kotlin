plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:util.runtime"))
    api(project(":compiler:config"))
    api(project(":compiler:config.jvm"))
    api(project(":js:js.config"))
    api(project(":native:kotlin-native-utils"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.getByName<Jar>("jar") {
    //excludes unused bunch files
    exclude("META-INF/extensions/*.xml.**")
}