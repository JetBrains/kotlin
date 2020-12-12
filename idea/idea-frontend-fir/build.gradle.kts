plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    compile(project(":idea:idea-frontend-independent"))
    compile(project(":idea:idea-frontend-api"))
    compile(project(":idea:idea-core"))
    compile(project(":compiler:fir:fir2ir"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:checkers"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:fir:jvm"))
    compile(project(":idea:idea-frontend-fir:idea-fir-low-level-api"))
    compile(intellijDep())
    compile(intellijCoreDep())

// <temp>
    compile(project(":idea:idea-core"))
// </temp>

    testCompile(project(":idea:idea-fir"))
    testCompile(intellijDep())
    testCompile(intellijCoreDep())
    testCompile(toolsJar())
    testCompile(project(":kotlin-reflect"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    compile(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
    doFirst {
        if (!kotlinBuildProperties.useFirIdeaPlugin) {
            error("Test task in the module should be executed with -Pidea.fir.plugin=true")
        }
    }
}

testsJar()

