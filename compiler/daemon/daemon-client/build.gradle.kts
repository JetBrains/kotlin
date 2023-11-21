description = "Kotlin Daemon Client"

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

dependencies {
    compileOnly(project(":daemon-common"))
    compileOnly(commonDependency("net.rubygrapefruit", "native-platform"))

    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(commonDependency("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embedded(commonDependency("net.rubygrapefruit", "native-platform", "-$it"))
    }
}

configureKotlinCompileTasksGradleCompatibility()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
