import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.protobuf") version "0.9.5"
    application
    id("me.champeau.jmh") version "0.7.2" //JMH
//    kotlin("kapt") // for JMH

    id("org.jetbrains.kotlinx.rpc.plugin") version "0.9.1"
}

application {
    mainClass.set("main.kotlin.server.RemoteCompilationServerKt")
}


group = "org.jetbrains.kotlin"

repositories {
    google()
    mavenCentral()
}

//rpc {
//    annotationTypeSafetyEnabled = true
//}

dependencies {
    compileOnly(project(":kotlin-daemon"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-compiler-runner-unshaded"))
    compileOnly(kotlin("stdlib"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":core:compiler.common"))
    compileOnly("it.unimi.dsi:fastutil:8.5.12")
    compileOnly(intellijCore())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")


//    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0")
//    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-compiler:2.2.0")

    implementation(project(":kotlin-daemon-client"))
    implementation("io.grpc:grpc-kotlin-stub:1.4.3")
    implementation("io.grpc:grpc-stub:1.74.0")
    implementation("io.grpc:grpc-protobuf:1.74.0")
    implementation("io.grpc:grpc-core:1.74.0")
    implementation("io.grpc:grpc-netty:1.74.0")
    implementation("com.google.protobuf:protobuf-kotlin:4.31.1")
    implementation("com.google.protobuf:protobuf-java-util:4.31.1") // printing default values of messages

    runtimeOnly(project(":kotlin-compiler-embeddable"))

    testImplementation(kotlin("test"))

    // gRPC testing utilities
    testImplementation("io.grpc:grpc-testing:1.74.0")
    testImplementation("io.grpc:grpc-inprocess:1.74.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // TAR
    implementation("org.apache.commons:commons-compress:1.24.0")

    // JMH
    implementation("org.openjdk.jmh:jmh-core:1.37")
    //kapt("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // koltinx rpc
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:0.9.1")
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-client:0.9.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-server:0.9.1")

    // ktor client
    implementation("io.ktor:ktor-client-cio-jvm:3.2.3")

    // protobuf serialization and its kotlinx-rpc integration
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-protobuf:0.9.1")

    // cbor serialization and its kotlinx-rpc integration
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-cbor:0.9.1")

    // json serialization and its kotlinx-rpc integration
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:0.9.1")

}

// added to resolve dependency issues with kotlinx-rpc dependencies
configurations {
    kotlinCompilerPluginClasspathMain.configure {
        resolutionStrategy.eachDependency {
            if ((requested.group == "org.jetbrains.kotlin" || requested.group == "org.jetbrains.kotlinx") && requested.name.startsWith("kotlinx-rpc")) {
                useTarget("${requested.group}:${requested.name}:2.2.0-0.9.1")
            } else if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
                useTarget("${requested.group}:${requested.name}:2.2.0")
            }
        }
    }
}

kotlin {
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion.set("2.2.0")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

// workaround that IDEA run configuration does not properly resolve runtime classpath
tasks.classes {
    dependsOn(configurations.runtimeClasspath)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.1"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.73.0"
        }

        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}