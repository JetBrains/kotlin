plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-jvm"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep())
    compile(project(":kotlin-native:kotlin-native-library-reader"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()

runtimeJar {
    archiveName = "native-ide.jar"
}
