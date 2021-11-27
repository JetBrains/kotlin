import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":kotlin-scripting-compiler"))
    testApi(projectTests(":compiler:tests-common"))
    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testApi(projectTests(":generators:test-generator"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(toolsJar())

    testRuntimeOnly(intellijPluginDep("java"))

    if (kotlinBuildProperties.isInJpsBuildIdeaSync)
        testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
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
