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
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }
    compile(project(":kotlin-native:kotlin-native-library-reader"))
    
    testCompileOnly(intellijDep())
    testRuntimeOnly(intellijDep())
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("gen")
    }
    "test" { projectDefault() }

}

configureFormInstrumentation()

runtimeJar {
    archiveName = "native-ide.jar"
}
