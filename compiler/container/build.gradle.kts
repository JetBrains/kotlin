
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
    compileOnly(project(":kotlin-stdlib"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(project(":kotlin-stdlib"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep()) { includeJars("trove4j") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
