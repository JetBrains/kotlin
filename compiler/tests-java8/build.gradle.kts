import kotlin.io.path.createTempDirectory

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":kotlin-scripting-compiler"))
    testApi(projectTests(":compiler:tests-common"))
    testImplementation(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testApi(projectTests(":generators:test-generator"))
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt")
val generateKotlinUseSiteFromJavaOnesForJspecifyTests by generator("org.jetbrains.kotlin.generators.tests.GenerateKotlinUseSitesFromJavaOnesForJspecifyTestsKt")

task<Exec>("downloadJspecifyTests") {
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

val test: Test by tasks

test.apply {
    exclude("**/*JspecifyAnnotationsTestGenerated*")
}

task<Test>("jspecifyTests") {
    workingDir(project.rootDir)
    include("**/*JspecifyAnnotationsTestGenerated*")
}

testsJar()
