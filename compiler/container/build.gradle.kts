
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
    compile(ideaSdkCoreDeps("intellij-core"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testRuntime(ideaSdkCoreDeps("trove4j", "intellij-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
}
