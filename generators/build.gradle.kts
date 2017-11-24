
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("jps-build-test")
}

dependencies {
    compile(protobufFull())
    compile(project(":idea"))
    compile(project(":j2k"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))
    compile(project(":idea:idea-test-framework"))
    compile(projectDist(":kotlin-test:kotlin-test-jvm"))
    compile(projectTests(":kotlin-build-common"))
    compile(projectTests(":compiler:tests-common"))
    compile(projectTests(":compiler:container"))
    compile(projectTests(":compiler:incremental-compilation-impl"))
    compile(projectTests(":compiler:cli"))
    compile(projectTests(":idea"))
    compile(projectTests(":idea:idea-gradle"))
    compile(projectTests(":idea:idea-maven"))
    compile(projectTests(":j2k"))
    compile(projectTests(":idea:idea-android"))
    compile(projectTests(":jps-plugin"))
    compile(projectTests(":plugins:android-extensions-compiler"))
    compile(projectTests(":plugins:android-extensions-ide"))
    compile(projectTests(":plugins:android-extensions-jps"))
    compile(projectTests(":kotlin-annotation-processing"))
    compile(projectTests(":kotlin-allopen-compiler-plugin"))
    compile(projectTests(":kotlin-noarg-compiler-plugin"))
    compile(projectTests(":kotlin-sam-with-receiver-compiler-plugin"))
    compile(projectTests(":plugins:uast-kotlin"))
    compile(projectTests(":js:js.tests"))
    compile(projectTests(":generators:test-generator"))
    compileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testRuntime(projectDist(":kotlin-reflect"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijExtra("jps-build-test"))
        testCompile(intellijExtra("jps-build-test"))
        testRuntime(intellij { include("idea_rt.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")
