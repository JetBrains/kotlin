import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:cli"))
    testImplementation(project(":compiler:ir.tree.impl"))
    testImplementation(project(":compiler:backend.jvm:backend.jvm.entrypoint"))
    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(projectTests(":generators:test-generator"))

    testApiJUnit5()
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-jvm6"))

    testRuntimeOnly(intellijDep()) {
        includeJars(
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            "jna",
            rootProject = rootProject
        )
    }

    Platform[202] {
        testRuntimeOnly(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-1") }
    }
    Platform[203].orHigher {
        testRuntimeOnly(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-3") }
    }
    testRuntimeOnly(toolsJar())
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
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
    dependsOn(":dist")
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"

    useJUnitPlatform()
}

testsJar()
