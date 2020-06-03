plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea:idea-frontend-fir"))
    compile(project(":idea:formatter"))
    compile(intellijDep())
    compile(intellijCoreDep())

// <temp>
    compile(project(":idea:idea-core"))
    compile(project(":idea"))
// </temp>

    testCompile(toolsJar())
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())
    testRuntime(intellijDep())

    Platform[192].orHigher {
        compile(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

if (rootProject.findProperty("idea.fir.plugin") == "true") {
    projectTest(parallel = true) {
        dependsOn(":dist")
        workingDir = rootDir
    }
}

testsJar()