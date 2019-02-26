plugins {
    kotlin("jvm")
}

group = "org.jetbrains.kotlin.fir"

dependencies {
    compile("org.antlr", "antlr4", "4.7.1")
    compile("org.antlr", "antlr4-runtime", "4.7.1")
    compile(project(":compiler:fir:tree"))
    testCompile("junit", "junit", "4.4")
    testCompile(projectTests(":compiler:fir:psi2fir"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}