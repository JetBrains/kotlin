
plugins {
    kotlin("jvm")
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-native-shared")
    compile(project(":kotlin-native:backend.native"))

    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep())
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("$rootDir/core/runtime.jvm/src")
    }
    "test" { none() }
}

configureInstrumentation()

runtimeJar {
    archiveName = "native-ide.jar"
}
