
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compile(projectTests(":compiler:cli"))
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
    compile(projectTests(":generators:test-generator"))
    compileOnly(intellijDep("jps-build-test"))
    compileOnly(project(":kotlin-reflect-api"))
    testCompile(intellijDep("jps-build-test"))
    testRuntime(intellijDep()) { includeJars("idea_rt") }
    testRuntime(projectDist(":kotlin-reflect"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")
