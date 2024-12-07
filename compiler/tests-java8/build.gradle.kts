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
    testCompileOnly(libs.junit4)
    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit5.get()}")
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testApi(projectTests(":generators:test-generator"))
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    useJUnitPlatform()
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

tasks.test {
    exclude("**/*JspecifyAnnotationsTestGenerated*")
}
task<Test>("jspecifyTests") {
    workingDir(project.rootDir)
    include("**/*JspecifyAnnotationsTestGenerated*")
}

testsJar()
