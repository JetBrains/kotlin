plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":compiler:util"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-gradle"))

    compile(androidDxJar())

    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("android"))
    compileOnly(intellijPluginDep("gradle"))

    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-gradle"))
    testCompile(commonDep("junit:junit"))

    testCompile(project(":idea:idea-native")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle-native")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-library-reader")) { isTransitive = false }
    testRuntime(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }

    testCompile(intellijDep())
    testCompile(intellijPluginDep("properties"))
    testCompileOnly(intellijPluginDep("android"))

    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    testRuntime(project(":kotlinx-serialization-ide-plugin"))

    testRuntime(intellijPluginDep("android"))

    testRuntime(intellijPluginDep("smali"))

    testRuntime(intellijPluginDep("copyright"))
    testRuntime(intellijPluginDep("coverage"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("IntelliLang"))
    testRuntime(intellijPluginDep("java-decompiler"))
    testRuntime(intellijPluginDep("java-i18n"))
    testRuntime(intellijPluginDep("junit"))

    Ide.IJ {
        testRuntime(intellijPluginDep("maven"))
    }

    testRuntime(intellijPluginDep("testng"))

    if (Ide.AS36.orHigher()) {
        testRuntime(intellijPluginDep("android-layoutlib"))
    }

    if (Ide.AS36()) {
        testRuntime(intellijPluginDep("android-wizardTemplate-plugin"))
    }
}

sourceSets {
    "main" { }
    "test" { }
}

projectTest(parallel = true) {
    workingDir = rootDir
    useAndroidSdk()
}

testsJar()

runtimeJar()
