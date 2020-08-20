plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    /*
    This module should not depend on compiler specific functionality
    No dependencies on descriptors (for now we still have transitive one by psi module), fir and similar modules,
    but may be dependencies on frontend independent one like psi & type-system
    This module is needed for smooth migration from descriptor frontend based IJ plugin to interlayer based one
     */

    compileOnly(project(":compiler:frontend")) // we need caches form here to work with ModuleInfo :(
    compileOnly(project(":idea:idea-jps-common"))

    compileOnly(project(":compiler:psi"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    compileOnly(intellijDep())
    compileOnly(project(":compiler:light-classes"))

    Platform[191].orLower {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    }
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDirs(
            "resources-en"
        )
    }
    "test" { projectDefault() }

}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
