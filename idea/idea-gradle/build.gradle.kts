plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compileOnly(project(":idea:idea-native")) { isTransitive = false }
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":idea:idea-gradle-tooling-api"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))

    compile(project(":js:js.frontend"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))
    Platform[193].orHigher {
        compileOnly(intellijPluginDep("gradle-java"))
    }
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijPluginDep("junit"))
    compileOnly(intellijPluginDep("testng"))

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
    testRuntime(project(":kotlin-native:kotlin-native-library-reader")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }

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
    testRuntime(project(":kotlin-gradle-plugin"))
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
    }
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))

    if (Ide.AS36.orHigher()) {
        testRuntime(intellijPluginDep("android-layoutlib"))
        testRuntime(intellijPluginDep("android-wizardTemplate-plugin"))
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

projectTest(parallel = true) {
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
