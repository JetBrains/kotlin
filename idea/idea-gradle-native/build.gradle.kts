plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-native"))

    compileOnly(project(":idea")) { isTransitive = false }
    compileOnly(project(":idea:idea-jvm"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))

    compile(project(":js:js.frontend"))

    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(projectTests(":idea:idea-gradle"))

    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("gradle-java"))
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijDep())

    testCompile(intellijPluginDep("gradle"))
    testCompile(intellijPluginDep("gradle-java"))
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijDep())

    compileOnly(intellijPluginDep("java"))

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":kotlin-gradle-statistics"))
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
    testRuntime(project(":plugins:parcelize:parcelize-ide"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))
    testRuntime(project(":plugins:lombok:lombok-ide-plugin"))
    // TODO: the order of the plugins matters here, consider avoiding order-dependency
    testRuntime(intellijPluginDep("java"))
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("testng"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("gradle-java"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage"))
    if (Ide.IJ()) {
        testRuntime(intellijPluginDep("maven"))
        testRuntime(intellijPluginDep("repository-search"))
    }
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))

    if (Ide.AS()) {
        testRuntime(intellijPluginDep("android-layoutlib"))
        testRuntime(intellijPluginDep("platform-images"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
    useAndroidSdk()
}

configureFormInstrumentation()

if (Ide.AS()) {
    getOrCreateTask<Test>("test") {
        setExcludes(listOf("**"))
    }
}
