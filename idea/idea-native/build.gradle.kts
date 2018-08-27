plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep())
    compile(project(":konan:konan-serializer"))
}

sourceSets {
    "main" {
        projectDefault()
//        java.srcDirs("$rootDir/core/runtime.jvm/src")
    }
    "test" { none() }
}

configureInstrumentation()

runtimeJar {
    archiveName = "native-ide.jar"
}
