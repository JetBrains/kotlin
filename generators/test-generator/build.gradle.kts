
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin()

dependencies {
    testCompile(project(":core:util.runtime"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(commonDep("junit:junit"))
}

afterEvaluate {
    dependencies {
        compile(intellij { include("util.jar") })
    }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
