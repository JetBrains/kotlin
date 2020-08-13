description = "Kotlin Daemon Client New"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val nativePlatformVariants = listOf(
    "windows-amd64",
    "windows-i386",
    "osx-amd64",
    "osx-i386",
    "linux-amd64",
    "linux-i386",
    "freebsd-amd64-libcpp",
    "freebsd-amd64-libstdcpp",
    "freebsd-i386-libcpp",
    "freebsd-i386-libstdcpp"
)

val ktorExcludesForDaemon : List<Pair<String, String>> by rootProject.extra

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":daemon-common-new"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":kotlin-daemon-client"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":daemon-common")) { isTransitive = false }
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

    embedded(project(":kotlin-daemon-client")) { isTransitive = false }
    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embedded(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) {
        isTransitive = false
    }
    compile(commonDep("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        apiVersion = "1.3"
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()

sourcesJar()

javadocJar()
