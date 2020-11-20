plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea"))
    compileOnly(intellijPluginDep("gradle"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-jvm"))
    compile(project(":compiler:frontend"))
    compile(project(":native:frontend.native"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))

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
