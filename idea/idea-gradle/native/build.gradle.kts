import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.github.jk1.tcdeps") version "0.17"
}

repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val kotlinNativeVersion = rootProject.extra["versions.kotlin.native"]

dependencies {
    compile(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxDist:$kotlinNativeVersion:shared.jar"))
    compile(tc("Kotlin_KotlinNative_Master_KotlinNativeLinuxDist:$kotlinNativeVersion:backend.native.jar"))

    compile("org.jetbrains.kotlin:kotlin-native-gradle-plugin:$kotlinNativeVersion") { isTransitive = false }

    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-native"))

    testRuntime(intellijDep())

    compileOnly(project(":idea")) { isTransitive = false }
    compileOnly(project(":idea:idea-jvm"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))

    compile(project(":js:js.frontend"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijPluginDep("junit"))

    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-test-framework"))

    testCompile(intellijPluginDep("gradle"))
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijDep())

    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":kotlin-scripting-idea"))
    // TODO: the order of the plugins matters here, consider avoiding order-dependency
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("testng"))
    testRuntime(intellijPluginDep("properties"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage"))
    testRuntime(intellijPluginDep("maven"))
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
