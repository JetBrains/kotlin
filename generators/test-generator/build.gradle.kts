
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    testCompile(project(":core:util.runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(commonDep("junit:junit"))
    testCompile(ideaSdkDeps("util"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
