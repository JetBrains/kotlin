plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":compiler:backend"))

    compileOnly(toolsJarApi())
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }

    testCompileOnly(toolsJarApi())
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile("org.jetbrains.intellij.deps:asm-all:9.1")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
