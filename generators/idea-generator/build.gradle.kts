plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { java.srcDirs("main") }
    "test" { projectDefault() }
}

dependencies {
    compile(kotlinStdlib("jdk8"))
    testCompile(projectTests(":idea:idea-maven"))
    testCompile(projectTests(":idea:idea-fir"))
    testCompile(projectTests(":idea:idea-fir-performance-tests"))
    testCompile(projectTests(":idea-frontend-fir"))
    testCompile(projectTests(":idea-frontend-fir:idea-fir-low-level-api"))
    testCompile(projectTests(":idea:idea-frontend-fir:fir-low-level-api-ide-impl"))
    testCompile(projectTests(":idea:idea-fir-fe10-binding"))
    testCompile(projectTests(":j2k"))
    testCompile(projectTests(":nj2k"))
    if (Ide.IJ()) {
        testCompile(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
        testCompile(projectTests(":idea:idea-new-project-wizard"))
    }
    testCompile(projectTests(":idea:performanceTests"))
    testCompile(projectTests(":idea:scripting-support"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(projectTests(":plugins:uast-kotlin"))
    testCompile(projectTests(":plugins:uast-kotlin-fir"))
    testCompile(projectTests(":idea:jvm-debugger:jvm-debugger-test"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-android"))
    testCompile(projectTests(":generators:test-generator"))
    testCompile(projectTests(":plugins:parcelize:parcelize-ide"))
    testCompile(projectTests(":kotlinx-serialization-ide-plugin"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-spec"))
    testApiJUnit5()
}

val generateIdeaTests by generator("org.jetbrains.kotlin.generators.tests.idea.GenerateTestsKt")
