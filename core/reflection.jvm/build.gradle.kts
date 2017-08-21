
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenLocal()
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath(ideaSdkDeps("asm-all"))
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
}

dependencies {
    val compile by configurations
    compile(project(":core:builtins"))
    compile(project(":core"))
    compile(protobufLite())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    dependsOn(protobufLiteTask)
}

tasks.withType<KotlinCompile> {
    dependsOn(protobufLiteTask)
}
