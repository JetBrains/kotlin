plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.5"
    application  // This plugin is required
}

application {
    mainClass.set("org.jetbrains.kotlin.server.RemoteKotlinDaemonKt")  // Your main class
}


group = "org.jetbrains.kotlin"
version = "2.3.255-SNAPSHOT"


repositories {
    mavenCentral()
    mavenCentral()

}

dependencies {
    api(project(":compiler:daemon"))
    api(project(":daemon-common"))
    api(project(":kotlin-daemon-client"))
    api(project(":kotlin-compiler-runner-unshaded"))
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    api(project(":compiler:cli"))
    api(project(":compiler:incremental-compilation-impl"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:util"))
    api(project(":core:compiler.common"))
    implementation("it.unimi.dsi:fastutil:8.5.12")

    runtimeOnly(intellijCore())
    runtimeOnly(project(":kotlin-compiler-embeddable"))


//    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0")


//    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
        // or
//    implementation("org.jetbrains.kotlin:kotlin-compiler:2.2.0")


    implementation("io.grpc:grpc-kotlin-stub:1.4.3")
    implementation("io.grpc:grpc-stub:1.73.0")
    implementation("io.grpc:grpc-protobuf:1.73.0")
    implementation("io.grpc:grpc-netty:1.73.0")

    implementation("com.google.protobuf:protobuf-kotlin:4.31.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.1"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.73.0"
        }

        create("grpckt") {  // Add the Kotlin gRPC plugin
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