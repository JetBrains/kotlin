plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

val antLauncherJar by configurations.creating

dependencies {
    testImplementation(intellijCore()) // Should come before compiler, because of "progarded" stuff needed for tests

    testApi(project(":kotlin-script-runtime"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    
    testApi(kotlinStdlib())

    testApi(commonDependency("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:fir:raw-fir:psi2fir"))
    testApi(projectTests(":compiler:fir:raw-fir:light-tree2fir"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testApi(projectTests(":generators:test-generator"))
    testApi(project(":compiler:ir.ir2cfg"))
    testApi(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":kotlin-script-util"))

    otherCompilerModules.forEach {
        testCompileOnly(project(it))
    }

    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(toolsJar())

    antLauncherJar(commonDependency("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" {}
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(
    parallel = true,
    defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)
) {
    dependsOn(":dist")

    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    val antLauncherJarPathProvider = project.provider {
        antLauncherJar.asPath
    }
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJarPathProvider.get())
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }
}

val generateTestData by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestDataKt")

testsJar()
