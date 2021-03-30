import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(project(":compiler:fir:raw-fir:psi2fir"))

    testImplementation(project(":compiler:visualizer:render-psi"))
    testImplementation(project(":compiler:visualizer:render-fir"))

    testApiJUnit5()

    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true, jUnit5Enabled = true) {
    workingDir = rootDir

    useJUnitPlatform()
}

testsJar()
