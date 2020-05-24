plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompileOnly(toolsJar())
    testRuntimeOnly(toolsJar())

    testRuntimeOnly(intellijPluginDep("gradle"))
    testCompile(project(":idea"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())
    testRuntime(intellijDep())

    if (Platform[191].orLower()) {
        testRuntimeOnly(intellijPluginDep("Groovy"))
    }
    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java"))
        testRuntime(intellijPluginDep("java"))
    }

    testRuntime(intellijRuntimeAnnotations())
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-preloader"))

    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin")) { isTransitive = false }

    testRuntime(project(":plugins:android-extensions-compiler"))
    testRuntimeOnly(project(":kotlin-android-extensions-runtime")) // TODO: fix import (workaround for jps build)
    testRuntime(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testRuntime(project(":allopen-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-allopen-compiler-plugin"))
    testRuntime(project(":noarg-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-noarg-compiler-plugin"))
    testRuntime(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-idea")) { isTransitive = false }
    testRuntime(project(":kotlin-scripting-compiler-impl-unshaded"))
    testRuntime(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlinx-serialization-compiler-plugin"))
    testRuntime(project(":kotlinx-serialization-ide-plugin")) { isTransitive = false }
    testRuntime(project(":idea:idea-android")) { isTransitive = false }
    testRuntime(project(":plugins:lint")) { isTransitive = false }
    testRuntime(project(":plugins:uast-kotlin"))
    testRuntime(project(":nj2k:nj2k-services")) { isTransitive = false }
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
