
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
        classpath("org.jetbrains.kotlin:kotlin-compiler-embeddable:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

repositories {
    mavenCentral()
}

val protobufCfg = configurations.create("protobuf-java")

dependencies {
    "protobuf-java"("com.google.protobuf:protobuf-java:2.6.1")
    "protobuf-java"("org.fusesource.jansi:jansi:1.11")
}

task("prepare") {
    dependsOn(protobufCfg)
    val inputJar = protobufCfg.files.find { it.name.startsWith("protobuf-java") }?.let { it.canonicalPath } ?: throw Exception("protobuf-java jar not found")
    val outputJar = "$buildDir/jars/protobuf-2.6.1-lite.jar"
    inputs.file(inputJar)
    outputs.file(outputJar)
    doLast {
        K2JVMCompiler().exec(System.out,
                "-script",
                "$rootDir/generators/infrastructure/build-protobuf-lite.kts",
                inputJar,
                outputJar)
    }
}

