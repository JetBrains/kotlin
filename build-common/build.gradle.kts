
description = "Kotlin Build Common"

apply { plugin("kotlin") }

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(ideaSdkDeps("util"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:tests-common"))
    testCompile(protobufFull())
    testRuntime(projectDist(":kotlin-stdlib"))
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
