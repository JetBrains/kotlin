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
    compile(project(":compiler:fir:fir2ir:jvm-backend"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:checkers"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:fir:jvm"))
    implementation(project(":compiler:fir:entrypoint"))
    compile(intellijDep())
    compile(intellijCoreDep())

// <temp>
    compile(project(":idea:idea-core"))
// </temp>

// <neededFor>`AbstractFirLazyResolveTest` uses fir implementation of references which are not in classpath otherwise
    testRuntimeOnly(project(":idea:idea-frontend-fir"))
// </neededFor>

    testCompile(toolsJar())
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:analysis-tests"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    Platform[192].orHigher {
        compile(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

if (rootProject.findProperty("idea.fir.plugin") == "true") {
    projectTest {
        dependsOn(":dist")
        workingDir = rootDir
    }
}

testsJar()

