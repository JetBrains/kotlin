plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(kotlinStdlib())
    testApi(intellijCore())
    testApi(project(":core:compiler.common"))
    testApi(project(":compiler:config"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:frontend.common.jvm"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":generators:test-generator"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

val generateAndroidTests by generator("org.jetbrains.kotlin.android.tests.CodegenTestsOnAndroidGenerator") {
    workingDir = rootDir

    val destinationDirectory =
        rootProject.file("libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/codegen-tests").absolutePath

    val testDataDirectories = arrayOf(
        rootProject.file("compiler/testData/codegen/box").absolutePath,
        rootProject.file("compiler/testData/codegen/boxInline").absolutePath,
    )

    args(destinationDirectory, *testDataDirectories)
    dependsOn(":dist", ":createIdeaHomeForTests")
    systemProperty("idea.home.path", ideaHomePathForTests().get().asFile.canonicalPath)
    systemProperty("idea.use.native.fs.for.win", false)
}
