
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:cli"))
    compile(project(":kotlin-build-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(projectTests(":kotlin-build-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()
