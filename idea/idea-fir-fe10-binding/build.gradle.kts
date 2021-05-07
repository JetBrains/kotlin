plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":idea:idea-frontend-fir"))

    testImplementation(projectTests(":idea:idea-fir"))
    testImplementation(projectTests(":idea:idea-frontend-independent"))

    testImplementation(intellijDep())
    testImplementation(intellijCoreDep())
    testImplementation(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
    val useFirIdeaPlugin = kotlinBuildProperties.useFirIdeaPlugin
    doFirst {
        if (!useFirIdeaPlugin) {
            error("Test task in the module should be executed with -Pidea.fir.plugin=true")
        }
    }
}
