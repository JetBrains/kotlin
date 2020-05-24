import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.io.File

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

val effectSystemEnabled: Boolean by rootProject.extra
val newInferenceEnabled: Boolean by rootProject.extra

configureFreeCompilerArg(effectSystemEnabled, "-Xeffect-system")
configureFreeCompilerArg(newInferenceEnabled, "-Xnew-inference")
configureFreeCompilerArg(true, "-Xuse-mixed-named-arguments")

fun configureFreeCompilerArg(isEnabled: Boolean, compilerArgument: String) {
    if (isEnabled) {
        allprojects {
            tasks.withType<KotlinCompile<*>> {
                kotlinOptions {
                    freeCompilerArgs += listOf(compilerArgument)
                }
            }
        }
    }
}

val antLauncherJar by configurations.creating

dependencies {
    testRuntime(intellijDep()) // Should come before compiler, because of "progarded" stuff needed for tests

    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    
    testCompile(kotlinStdlib())

    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:fir:raw-fir:psi2fir"))
    testCompile(projectTests(":compiler:fir:raw-fir:light-tree2fir"))
    testCompile(projectTests(":compiler:fir:fir2ir"))
    testCompile(projectTests(":compiler:fir:analysis-tests"))
    testCompile(projectTests(":compiler:visualizer"))
    testCompile(projectTests(":generators:test-generator"))
    testCompile(project(":compiler:ir.ir2cfg"))
    testCompile(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testCompile(project(":kotlin-scripting-compiler-unshaded"))
    testCompile(project(":kotlin-script-util"))
    testCompileOnly(project(":kotlin-reflect-api"))
    otherCompilerModules.forEach {
        testCompileOnly(project(it))
    }
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    Platform[193].orLower {
        testCompileOnly(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "util", "asm-all", rootProject = rootProject) }

    Platform[192].orHigher {
        testRuntimeOnly(intellijPluginDep("java"))
    }

    testRuntime(project(":kotlin-reflect"))
    testRuntime(androidDxJar())
    testRuntime(toolsJar())

    antLauncherJar(commonDep("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
    }
}

projectTest(parallel = true) {
    dependsOn(":dist")

    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJar.asPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsKt")

testsJar()
