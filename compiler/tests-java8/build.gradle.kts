plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":kotlin-scripting-compiler"))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(project(":compiler:cli-jvm:javac-integration"))
    testFixturesImplementation(intellijCore())
    testImplementation(intellijCore())
    testFixturesApi(platform(libs.junit.bom))
    testCompileOnly(libs.junit4)
    testFixturesImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit5.get()}")
    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit5.get()}")
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/builtin-classes")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withScriptingPlugin()
    withTestJar()
    withAnnotations()
    withMockJdkAnnotationsJar()
    withThirdPartyJava8Annotations()

    testTask(
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_21_0),
        jUnitMode = JUnitMode.JUnit5,
        javaLauncher = JdkMajorVersion.JDK_1_8
    ) {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    }
}


optInToK1Deprecation()

testsJar()
