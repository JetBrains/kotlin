plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.5"
    id("org.jetbrains.kotlin.plugin.serialization")
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
        // or
//    implementation("org.jetbrains.kotlin:kotlin-compiler:2.2.0")

    implementation(project(":kotlin-daemon-client"))
    implementation("io.grpc:grpc-kotlin-stub:1.4.3")
    implementation("io.grpc:grpc-stub:1.73.0")
    implementation("io.grpc:grpc-protobuf:1.73.0")
    implementation("io.grpc:grpc-netty:1.73.0")

    implementation("com.google.protobuf:protobuf-kotlin:4.31.1")

    runtimeOnly(project(":kotlin-compiler-embeddable"))

    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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