
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(kotlinStdlib())
    compile(project(":kotlin-reflect"))
    compile(projectTests(":compiler:tests-common"))
    compile(commonDep("junit:junit"))
    compileOnly(intellijDep()) { includeJars("openapi") }

    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
    testCompile(intellijDep()) { includeJars("openapi", "util", "idea", "idea_rt", "groovy-all", "jps-builders", rootProject = rootProject) }
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
