
description = "Kotlin Build Common"

apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":kotlin-reflect-api"))
    testCompileOnly(project(":compiler:cli-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(protobufFull())
    testCompile(projectDist(":kotlin-stdlib"))
    testCompileOnly(ideaSdkDeps("openapi"))
    testRuntime(projectDist(":kotlin-reflect"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("util.jar") })
        testCompileOnly(intellij { include("openapi.jar") })
    }
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
