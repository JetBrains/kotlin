
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(intellijDep()) { includeJars("util") }
    testCompile(project(":core:util.runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
