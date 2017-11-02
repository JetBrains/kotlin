
apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("android", "coverage", "gradle", "Groovy", "junit", "maven", "properties", "testng")
}

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-build-common"))

    compile(project(":js:js.frontend"))

    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-jps-common"))

    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":idea:idea-test-framework"))

    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "idea.jar", "gson-*.jar") })
        compileOnly(intellijPlugin("maven") { include("maven.jar", "maven-server-api.jar") })
        testCompileOnly(intellij { include("openapi.jar", "idea.jar", "gson-*.jar", "idea_rt.jar") })
        testCompileOnly(intellijPlugin("maven") { include("maven.jar", "maven-server-api.jar") })
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
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    workingDir = rootDir
}
