import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    compile(project(":idea:idea-frontend-independent"))
    compile(project(":idea:idea-frontend-api"))
    compile(project(":idea:idea-core"))
    compile(project(":compiler:fir:fir2ir"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:fir:resolve"))
    compile(project(":compiler:fir:checkers"))
    compile(project(":compiler:fir:java"))
    compile(project(":compiler:fir:jvm"))
    compile(project(":idea:idea-frontend-fir:idea-fir-low-level-api"))
    compile(intellijDep())
    compile(intellijCoreDep())

// <temp>
    compile(project(":idea:idea-core"))
// </temp>

    testCompile(project(":idea:idea-fir"))
    testCompile(intellijDep())
    testCompile(intellijCoreDep())
    testCompile(toolsJar())
    testCompile(project(":kotlin-reflect"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":idea:idea-frontend-independent"))

    compile(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
    val useFirIdeaPlugin = kotlinBuildProperties.useFirIdeaPlugin
    doFirst {
        if (!useFirIdeaPlugin) {
            error("Test task in the module should be executed with -Pidea.fir.plugin=true")
        }
    }
}

testsJar()

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("idea-frontend-fir-generator"))
}

val generateCode by tasks.registering(NoDebugJavaExec::class) {
    val generatorRoot = "$projectDir/idea/idea-frontend-fir/idea-frontend-fir-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)

    workingDir = rootDir
    classpath = generatorClasspath
    main = "org.jetbrains.kotlin.idea.frontend.api.fir.generator.MainKt"
    systemProperties["line.separator"] = "\n"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateCode)


