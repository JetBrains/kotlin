
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

val builtinsSourceSet = sourceSets.create("builtins") {
    java.srcDir("builtins")
}
val builtinsCompile by configurations

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
    builtinsCompile("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion")
    testCompileOnly(intellijDep("jps-build-test"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(intellijDep("jps-build-test"))
    testCompile(builtinsSourceSet.output)
    testRuntime(intellijDep()) { includeJars("idea_rt") }
    testRuntime(projectDist(":kotlin-reflect"))
}


projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")

val generateBuiltins by generator("org.jetbrains.kotlin.generators.builtins.generateBuiltIns.GenerateBuiltInsKt", builtinsSourceSet)

testsJar()
