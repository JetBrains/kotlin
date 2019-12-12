plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":libraries:tools:new-project-wizard"))
    implementation(project(":idea:ide-common"))
    implementation(project(":idea:idea-core"))
    implementation(project(":idea:idea-jvm"))
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(intellijCoreDep())
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))

    excludeInAndroidStudio(rootProject) {
        compileOnly(intellijPluginDep("maven"))
    }

    Platform[191].orLower {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
