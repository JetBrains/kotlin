import TaskUtils.useAndroidEmulator

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":compiler:frontend.java"))

    testCompile(kotlinStdlib())
    testCompile(project(":kotlin-reflect"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))

    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))

    Platform[193].orLower {
        testCompile(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }

    testCompile(intellijDep()) { includeJars("util", "idea", "idea_rt", rootProject = rootProject) }
    Platform[202].orHigher {
        testCompile(intellijDep()) { includeJars("groovy", rootProject = rootProject) }
    }
    Platform[201].orLower {
        testCompile(intellijDep()) { includeJars("groovy-all", rootProject = rootProject) }
    }
    Platform[192].orHigher {
        testCompile(intellijPluginDep("java")) { includeJars("jps-builders") }
    }
    testCompile(jpsStandalone()) { includeJars("jps-model") }
    testCompile(jpsBuildTest())
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    doFirst {
        environment("kotlin.tests.android.timeout", "45")
    }

    if (project.hasProperty("teamcity") || project.hasProperty("kotlin.test.android.teamcity")) {
        systemProperty("kotlin.test.android.teamcity", true)
    }

    project.findProperty("kotlin.test.android.path.filter")?.let {
        systemProperty("kotlin.test.android.path.filter", it.toString())
    }

    workingDir = rootDir
    useAndroidEmulator(this)
}

val generateAndroidTests by generator("org.jetbrains.kotlin.android.tests.CodegenTestsOnAndroidGenerator")

generateAndroidTests.workingDir = rootDir
generateAndroidTests.dependsOn(rootProject.tasks.named("dist"))
