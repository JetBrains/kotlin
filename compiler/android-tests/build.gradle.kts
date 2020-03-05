
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(kotlinStdlib())
    compile(project(":kotlin-reflect"))
    compile(projectTests(":compiler:tests-common"))
    compile(commonDep("junit:junit"))

    Platform[193].orLower {
        compileOnly(intellijDep()) { includeJars("openapi") }
    }

    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
    Platform[193].orLower {
        testCompile(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }

    testCompile(intellijDep()) { includeJars("util", "idea", "idea_rt", "groovy-all", rootProject = rootProject) }
    Platform[191].orLower {
        testCompile(intellijDep()) { includeJars("jps-builders") }
    }
    Platform[192].orHigher {
        testCompile(intellijPluginDep("java")) { includeJars("jps-builders") }
    }
    testCompile(jpsStandalone()) { includeJars("jps-model") }
    testCompile(jpsBuildTest())
}

sourceSets {
    "main" { projectDefault() }
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
}

val generateAndroidTests by generator("org.jetbrains.kotlin.android.tests.CodegenTestsOnAndroidGenerator")

generateAndroidTests.workingDir = rootDir
generateAndroidTests.dependsOn(rootProject.tasks.named("dist"))
