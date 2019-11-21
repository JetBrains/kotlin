plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompileOnly(intellijDep())

    testCompile(project(":idea:jvm-debugger:jvm-debugger-core"))
    testCompile(project(":idea:jvm-debugger:jvm-debugger-evaluation"))
    testCompile(project(":idea:jvm-debugger:jvm-debugger-sequence"))
    testCompile(project(":compiler:backend"))
    testCompile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(commonDep("junit:junit"))

    testCompile(intellijPluginDep("stream-debugger"))

    Platform[191].orLower {
        testCompileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl", "aether-dependency-resolver") }
        testRuntime(intellijPluginDep("java"))
    }

    testRuntime(project(":nj2k:nj2k-services")) { isTransitive = false }
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-native")) { isTransitive = false }
    testRuntime(project(":idea:idea-gradle-native")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-library-reader")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }

    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))

    testRuntime(intellijDep())
    testRuntime(intellijRuntimeAnnotations())
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