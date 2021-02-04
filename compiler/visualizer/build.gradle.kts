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

    testApi(platform("org.junit:junit-bom:5.7.0"))
    testApi("org.junit.jupiter:junit-jupiter")
    testApi("org.junit.platform:junit-platform-commons:1.7.0")
    testApi("org.junit.platform:junit-platform-launcher:1.7.0")

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
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
