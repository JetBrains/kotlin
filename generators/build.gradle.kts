plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { java.srcDirs("main") }
    "test" { projectDefault() }
}

fun extraSourceSet(name: String, extendMain: Boolean = true): Pair<SourceSet, Configuration> {
    val sourceSet = sourceSets.create(name) {
        java.srcDir(name)
    }
    val api = configurations[sourceSet.apiConfigurationName]
    if (extendMain) {
        dependencies { api(mainSourceSet.output) }
        configurations[sourceSet.runtimeOnlyConfigurationName]
            .extendsFrom(configurations.runtimeClasspath.get())
    }
    return sourceSet to api
}

val (builtinsSourceSet, builtinsApi) = extraSourceSet("builtins", extendMain = false)
val (evaluateSourceSet, evaluateApi) = extraSourceSet("evaluate")

dependencies {
    // for GeneratorsFileUtil
    compile(kotlinStdlib())
    compile(intellijDep()) { includeJars("util") }

    builtinsApi("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion") { isTransitive = false }
    evaluateApi(project(":core:deserialization"))

    testCompile(builtinsSourceSet.output)
    testCompile(evaluateSourceSet.output)

    testCompile(projectTests(":compiler:cli"))
    testCompile(projectTests(":idea:idea-maven"))
    testCompile(projectTests(":idea:idea-fir"))
    testCompile(projectTests(":idea:idea-frontend-fir"))
    testCompile(projectTests(":j2k"))
    testCompile(projectTests(":nj2k"))
    if (Ide.IJ()) {
        testCompile(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
        testCompile(projectTests(":idea:idea-new-project-wizard"))
    }
    testCompile(projectTests(":idea:idea-android"))
    testCompile(projectTests(":idea:performanceTests"))
    testCompile(projectTests(":idea:scripting-support"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(projectTests(":plugins:jvm-abi-gen"))
    testCompile(projectTests(":plugins:android-extensions-compiler"))
    testCompile(projectTests(":plugins:android-extensions-ide"))
    testCompile(projectTests(":kotlin-annotation-processing"))
    testCompile(projectTests(":kotlin-annotation-processing-cli"))
    testCompile(projectTests(":kotlin-allopen-compiler-plugin"))
    testCompile(projectTests(":kotlin-noarg-compiler-plugin"))
    testCompile(projectTests(":kotlin-sam-with-receiver-compiler-plugin"))
    testCompile(projectTests(":kotlinx-serialization-compiler-plugin"))
    testCompile(projectTests(":plugins:fir:fir-plugin-prototype"))
    testCompile(projectTests(":idea:jvm-debugger:jvm-debugger-test"))
    testCompile(projectTests(":generators:test-generator"))
    testCompile(projectTests(":idea"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntime(intellijDep()) { includeJars("idea_rt") }
    testRuntime(project(":kotlin-reflect"))

    if (Ide.IJ()) {
        testCompileOnly(jpsBuildTest())
        testCompile(jpsBuildTest())
    }
}


projectTest(parallel = true) {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")
val generateKeywordStrings by generator("org.jetbrains.kotlin.generators.frontend.GenerateKeywordStrings")

val generateBuiltins by generator("org.jetbrains.kotlin.generators.builtins.generateBuiltIns.GenerateBuiltInsKt", builtinsSourceSet)
val generateOperationsMap by generator("org.jetbrains.kotlin.generators.evaluate.GenerateOperationsMapKt", evaluateSourceSet)

testsJar()
