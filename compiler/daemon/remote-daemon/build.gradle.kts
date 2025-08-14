plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.protobuf") version "0.9.5"
    id("org.jetbrains.kotlin.plugin.serialization")
    application  // This plugin is required
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.9.1"
}

application {
    mainClass.set("org.jetbrains.kotlin.server.RemoteKotlinDaemonKt")
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    runtimeOnly(project(":kotlin-compiler-embeddable"))

    testImplementation(kotlin("test"))
    testImplementation("io.grpc:grpc-testing:1.74.0")
    testImplementation("io.grpc:grpc-inprocess:1.74.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

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
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:0.9.1")

    kotlinCompilerPluginClasspathMain("org.jetbrains.kotlinx:kotlinx-rpc-compiler-plugin-k2:2.2.0-0.9.1!")
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