import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(project(":compiler:fir:raw-fir:psi2fir"))

    testCompileOnly(project(":compiler:visualizer:render-psi"))
    testCompileOnly(project(":compiler:visualizer:render-fir"))

    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:analysis-tests"))
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

projectTest {
    workingDir = rootDir
}

testsJar()
