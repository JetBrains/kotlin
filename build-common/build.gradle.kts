
description = "Kotlin Build Common"

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(ideaSdkDeps("util"))
    testCompileOnly(project(":compiler:cli-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(protobufFull())
    testCompile(projectDist(":kotlin-stdlib"))
    testCompileOnly(ideaSdkDeps("openapi"))
    testRuntime(projectDist(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest()

publish()
