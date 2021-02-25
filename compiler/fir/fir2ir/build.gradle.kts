import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.psi2ir"))
    compileOnly(project(":compiler:ir.backend.common"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(intellijDep())

    testRuntimeOnly(intellijDep())

    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:fir:analysis-tests"))
    testApi(project(":compiler:fir:fir-serialization"))

    testApiJUnit5()

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir2ir:jvm-backend"))

    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
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

projectTest(jUnit5Enabled = true) {
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()
