import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("com.google.protobuf") version "0.9.5"
}

configureKotlinCompileTasksGradleCompatibility()

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        @Suppress("DEPRECATION")
        apiVersion.set(KotlinVersion.KOTLIN_1_7)
    }
}

protobuf {
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:4.30.2"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.71.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc")
                create("grpckt")
            }
            builtins {
                create("kotlin")
            }
        }
    }
}


dependencies {
    compileOnly(kotlinStdlib())
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    embedded(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))

    implementation("com.google.protobuf:protobuf-kotlin:4.30.2")
    implementation("com.google.protobuf:protobuf-java-util:4.30.2")
    implementation("io.grpc:grpc-protobuf:1.71.0")
    implementation("io.grpc:grpc-netty:1.71.0")
    implementation("io.grpc:grpc-inprocess:1.71.0")
    implementation("io.grpc:grpc-services:1.71.0")
    implementation("io.grpc:grpc-stub:1.71.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(kotlinStdlib())
    testImplementation(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
}

kotlin {
//    explicitApi()
}

publish()

standardPublicJars()

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
//        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
}