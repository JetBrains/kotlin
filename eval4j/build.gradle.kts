
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":compiler:backend"))
    compile(files(toolsJar()))
    compileOnly(intellijDep()) { includeJars("asm-all") }
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(intellijDep()) { includeJars("asm-all") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
