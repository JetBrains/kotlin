
description = "Kotlin Daemon Client"

apply { plugin("kotlin") }

jvmTarget = "1.6"

val nativePlatformUberjar = preloadedDeps("native-platform-uberjar")

val packIntoJar by configurations.creating

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(nativePlatformUberjar)
    packIntoJar(projectClasses(":compiler:daemon-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    nativePlatformUberjar.forEach {
        from(zipTree(it))
    }
    from(packIntoJar)
}
sourcesJar()
javadocJar()

dist()

publish()
