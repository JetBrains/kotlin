plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    api(project(":compiler:compiler.version"))
    api(project(":core:util.runtime"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:log4j"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(jpsModel()) { isTransitive = false }
    compileOnly(jpsModelImpl()) { isTransitive = false }

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources"))
    }
    "test" {
        projectDefault()
    }
}

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}