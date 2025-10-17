import kotlin.io.path.createTempDirectory

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":kotlin-scripting-compiler"))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
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
    testData(project(":compiler").isolated, "testData/loadJava")
    testData(project(":compiler").isolated, "testData/loadJava8")
    testData(project(":compiler").isolated, "testData/resolvedCalls/enhancedSignatures")
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
        jUnitMode = JUnitMode.JUnit5
    ) {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt", generateTestsInBuildDirectory = true)
}


optInToK1Deprecation()

tasks.register<Exec>("downloadJspecifyTests") {
    val tmpDirPath = createTempDirectory().toAbsolutePath().toString()
    doFirst {
        executable("git")
        args("clone", "https://github.com/jspecify/jspecify/", tmpDirPath)
    }
    doLast {
        copy {
            from("$tmpDirPath/samples")
            into("${project.rootDir}/compiler/testData/foreignAnnotationsJava8/tests/jspecify/java")
        }
    }
}

testsJar()
