apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("android", "coverage", "gradle", "Groovy", "junit", "maven", "properties", "testng")
}

val androidSdk by configurations.creating

dependencies {

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))

    compile(project(":js:js.frontend"))

    testCompile(projectTests(":idea"))
    testCompile(project(":idea:idea-test-framework"))

    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    androidSdk(project(":custom-dependencies:android-sdk", configuration = "androidSdk"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "idea.jar", "external-system-rt.jar", "forms_rt.jar", "extensions.jar", "jdom.jar", "util.jar") })
        compileOnly(intellijPlugin("gradle") { include("gradle-tooling-api-*.jar", "gradle.jar", "gradle-base-services-*.jar") })
        compileOnly(intellijPlugin("Groovy") { include("Groovy.jar") })
        compileOnly(intellijPlugin("junit") { include("idea-junit.jar") })
        testCompile(intellijPlugin("gradle") { include("gradle-wrapper-*.jar", "gradle-base-services-*.jar", "gradle-tooling-extension-impl.jar", "gradle-tooling-api-*.jar", "gradle.jar") })
        testCompileOnly(intellijPlugin("Groovy") { include("Groovy.jar") })
        testCompileOnly(intellij { include("groovy-all-*.jar", "idea_rt.jar") })
        testRuntime(intellij())
        // TODO: the order of the plugins matters here, consider avoiding order-dependency
        testRuntime(intellijPlugins("junit"))
        testRuntime(intellijPlugin("testng") { include("jcommander.jar", "resources_en.jar") })
        testRuntime(intellijPlugin("properties") { include("resources_en.jar") })
        testRuntime(intellijPlugins("gradle", "Groovy"))
        testRuntime(intellijPlugin("coverage") { include("jacocoant*.jar") })
        testRuntime(intellijPlugins("maven", "android"))
    }
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir("res").apply { include("**") }
    }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    workingDir = rootDir
    dependsOn(androidSdk)
    doFirst {
        systemProperty("android.sdk", androidSdk.singleFile.canonicalPath)
    }
}

configureInstrumentation()
