plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.serialization.js"))
    compile(project(":compiler:backend.js"))
    compile(project(":js:js.translator"))
    compile(project(":js:js.serializer"))
    compile(project(":js:js.dce"))

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
}
