
apply { plugin("kotlin") }

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

    compileOnly(intellijDep()) { includeJars("openapi", "idea", "gson-2.5", "jdom", "extensions", "util") }
    compileOnly(intellijPluginDep("maven")) { includeJars("maven", "maven-server-api") }

    testCompile(projectTests(":idea"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":idea:idea-test-framework"))

    testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "gson-2.5", "idea_rt") }
    testCompileOnly(intellijPluginDep("maven")) { includeJars("maven", "maven-server-api") }

    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))

    testRuntime(intellijDep())
    // TODO: the order of the plugins matters here, consider avoiding order-dependency
    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("testng")) { includeJars("jcommander", "resources_en") }
    testRuntime(intellijPluginDep("properties")) { includeJars("resources_en") }
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage")) { includeJars("jacocoant") }
    testRuntime(intellijPluginDep("maven"))
    testRuntime(intellijPluginDep("android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    workingDir = rootDir
}
