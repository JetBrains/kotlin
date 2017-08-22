
description = "Kotlin Daemon Client"

apply { plugin("kotlin") }

jvmTarget = "1.6"

val nativePlatformUberjar = preloadedDeps("native-platform-uberjar")

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(nativePlatformUberjar)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    nativePlatformUberjar.forEach {
        from(zipTree(it))
    }
}
sourcesJar()

dist()

