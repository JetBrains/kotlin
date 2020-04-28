plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compileOnly(project(":idea:idea-native"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))

    compile(project(":js:js.frontend"))

    compile(project(":native:frontend.native"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))
    Platform[193].orHigher {
        compileOnly(intellijPluginDep("gradle-java"))
    }
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijPluginDep("junit"))
    compileOnly(intellijPluginDep("testng"))
    runtimeOnly(project(":kotlin-coroutines-experimental-compat"))

    compileOnly(project(":kotlin-gradle-statistics"))

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
        testCompileOnly(intellijPluginDep("java"))
        testRuntimeOnly(intellijPluginDep("java"))
    }

    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-test-framework"))

    testCompile(intellijPluginDep("gradle"))
    Platform[193].orHigher {
        testCompile(intellijPluginDep("gradle-java"))
    }
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijDep())

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }
    if (Ide.IJ()) {
        testRuntime(project(":idea:idea-new-project-wizard"))
    }

    testRuntimeOnly(toolsJar())
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
    testRuntime(project(":kotlin-gradle-statistics"))
    // TODO: the order of the plugins matters here, consider avoiding order-dependency
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("testng"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("gradle"))
    Platform[193].orHigher {
        testRuntime(intellijPluginDep("gradle-java"))
    }
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage"))
    if (Ide.IJ()) {
        testRuntime(intellijPluginDep("maven"))

        if (Ide.IJ201.orHigher()) {
            testRuntime(intellijPluginDep("repository-search"))
        }
    }
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))

    if (Ide.AS36.orHigher()) {
        testRuntime(intellijPluginDep("android-layoutlib"))
    }
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir("res")
    }
    "test" { projectDefault() }
}

testsJar()

projectTest(parallel = false) {
    workingDir = rootDir
    useAndroidSdk()

    doFirst {
        val mainResourceDirPath = File(project.buildDir, "resources/main").absolutePath
        sourceSets["test"].runtimeClasspath = sourceSets["test"].runtimeClasspath.filter { file ->
            if (!file.absolutePath.contains(mainResourceDirPath)) {
                true
            } else {
                println("Remove `${file.path}` from the test runtime classpath")
                false
            }
        }
    }
}

configureFormInstrumentation()
