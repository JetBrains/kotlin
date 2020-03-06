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

    testImplementation(projectTests(":idea"))
    testImplementation(project(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
    testImplementation(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(intellijDep())
    testImplementation(intellijPluginDep("gradle"))


    excludeInAndroidStudio(rootProject) {
        compileOnly(intellijPluginDep("maven"))
    }

    Platform[191].orLower {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testRuntimeOnly(intellijPluginDep("java")) { includeJars("java-api") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
