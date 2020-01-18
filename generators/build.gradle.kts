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
    api(kotlinStdlib())
    api(intellijDep()) { includeJars("util") }

    builtinsApi("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion") { isTransitive = false }
    evaluateApi(project(":core:deserialization"))

    testImplementation(builtinsSourceSet.output)
    testImplementation(evaluateSourceSet.output)

    testImplementation(projectTests(":compiler:cli"))
    testImplementation(projectTests(":idea:idea-maven"))
    testImplementation(projectTests(":j2k"))
    testImplementation(projectTests(":nj2k"))
    testImplementation(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
    testImplementation(projectTests(":idea:idea-android"))
    testImplementation(projectTests(":idea:scripting-support"))
    testImplementation(projectTests(":jps-plugin"))
    testImplementation(projectTests(":plugins:jvm-abi-gen"))
    testImplementation(projectTests(":plugins:android-extensions-compiler"))
    testImplementation(projectTests(":plugins:android-extensions-ide"))
    testImplementation(projectTests(":kotlin-annotation-processing"))
    testImplementation(projectTests(":kotlin-annotation-processing-cli"))
    testImplementation(projectTests(":kotlin-allopen-compiler-plugin"))
    testImplementation(projectTests(":kotlin-noarg-compiler-plugin"))
    testImplementation(projectTests(":kotlin-sam-with-receiver-compiler-plugin"))
    testImplementation(projectTests(":kotlinx-serialization-compiler-plugin"))
    testImplementation(projectTests(":idea:jvm-debugger:jvm-debugger-test"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":idea"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(intellijDep()) { includeJars("idea_rt") }
    testRuntimeOnly(project(":kotlin-reflect"))

    if (Ide.IJ()) {
        testCompileOnly(jpsBuildTest())
        testImplementation(jpsBuildTest())
    }
}


projectTest(parallel = true) {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")

val generateBuiltins by generator("org.jetbrains.kotlin.generators.builtins.generateBuiltIns.GenerateBuiltInsKt", builtinsSourceSet)
val generateOperationsMap by generator("org.jetbrains.kotlin.generators.evaluate.GenerateOperationsMapKt", evaluateSourceSet)

testsJar()
