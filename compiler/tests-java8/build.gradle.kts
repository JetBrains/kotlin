import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(projectTests(":generators:test-generator"))
    testRuntime(project(":kotlin-reflect"))
    testRuntimeOnly(toolsJar())
    testRuntime(intellijDep())
    testRuntimeOnly(intellijPluginDep("java"))
    if (isIdeaActive) testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    systemProperty("idea.home.path", intellijRootDir().canonicalPath)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt")
val generateKotlinUseSiteFromJavaOnesForJspecifyTests by generator("org.jetbrains.kotlin.generators.tests.GenerateKotlinUseSitesFromJavaOnesForJspecifyTestsKt")

task<Exec>("downloadJspecifyTests") {
    val tmpDirPath = createTempDir().absolutePath
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
