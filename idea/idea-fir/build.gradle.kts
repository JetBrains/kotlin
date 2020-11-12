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
    testCompile(projectTests(":idea:idea-frontend-fir"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":idea:idea-frontend-independent"))

    testCompileOnly(intellijDep())
    testRuntime(intellijDep())

    compile(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

if (kotlinBuildProperties.useFirIdeaPlugin) {
    projectTest(parallel = true) {
        dependsOn(":dist")
        workingDir = rootDir
    }
}

testsJar()
