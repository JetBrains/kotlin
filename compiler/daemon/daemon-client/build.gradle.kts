import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon Client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven("https://repo.gradle.org/gradle/ext-releases-local")
}

jvmTarget = "1.6"

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
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

    embeddedComponents(project(":compiler:daemon-common")) { isTransitive = false }
    embeddedComponents(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embeddedComponents(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

noDefaultJar()

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(mainSourceSet.output)
    fromEmbeddedComponents()
}

sourcesJar()

javadocJar()

dist()

ideaPlugin()

publish()
