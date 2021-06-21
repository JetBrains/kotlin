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

    testCompile(projectTests(":generators:test-generator"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-spec"))
    testCompile(projectTests(":idea-frontend-fir:idea-fir-low-level-api"))
    testCompile(projectTests(":idea-frontend-fir"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
    testApiJUnit5()
}

val generateFrontendApiTests by generator("org.jetbrains.kotlin.generators.tests.frontend.api.GenerateTestsKt")
