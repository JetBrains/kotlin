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
    Platform[193].orHigher {
        compileOnly(intellijPluginDep("gradle-java"))
    }

    testImplementation(projectTests(":idea"))
    testImplementation(project(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
    testImplementation(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(intellijDep())
    testImplementation(intellijPluginDep("gradle"))

    testImplementation(projectTests(":idea:idea-gradle"))

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":plugins:kapt3-idea"))
    testRuntimeOnly(project(":allopen-ide-plugin"))
    testRuntimeOnly(project(":sam-with-receiver-ide-plugin"))
    testRuntimeOnly(project(":noarg-ide-plugin"))
    testRuntimeOnly(project(":kotlinx-serialization-ide-plugin"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    testRuntimeOnly(project(":kotlin-gradle-statistics"))
    testRuntimeOnly(project(":kotlin-scripting-idea"))
    testRuntimeOnly(project(":plugins:parcelize:parcelize-ide"))
    testRuntimeOnly(intellijRuntimeAnnotations())



    excludeInAndroidStudio(rootProject) {
        compileOnly(intellijPluginDep("maven"))
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
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
    systemProperty("cacheRedirectorEnabled", findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true)
}
