plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())
    testRuntime(intellijRuntimeAnnotations())

    testCompile(project(":compiler:backend"))
    testCompile(project(":idea:idea-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(projectTests(":idea"))
    testCompile(commonDep("junit:junit"))

    testRuntime(project(":allopen-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-allopen-compiler-plugin"))
    testRuntime(project(":noarg-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-noarg-compiler-plugin"))
    testRuntime(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-idea")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-compiler-unshaded"))
    testRuntime(project(":kotlin-scripting-compiler-impl-unshaded"))
    testRuntime(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlinx-serialization-compiler-plugin"))
    testRuntime(project(":kotlinx-serialization-ide-plugin")) { isTransitive = false }

    testRuntime(project(":idea:idea-android")) { isTransitive = false }

    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java"))
        testRuntime(intellijPluginDep("java"))
    }

    testRuntimeOnly(toolsJar())
    testRuntime(project(":kotlin-reflect"))

    testCompileOnly(intellijDep())
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()