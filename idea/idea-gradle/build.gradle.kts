apply { plugin("kotlin") }

dependencies {

    compileOnly(ideaSdkDeps("openapi", "idea", "external-system-rt", "forms_rt"))
    compileOnly(ideaPluginDeps("gradle-tooling-api", "gradle", "gradle-base-services", plugin = "gradle"))
    compileOnly(ideaPluginDeps("Groovy", plugin = "Groovy"))

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))

    compile(project(":js:js.frontend"))

    testCompile(projectTests(":idea"))
    testCompile(project(":idea:idea-test-framework"))
    testCompile(ideaPluginDeps("gradle-wrapper", "gradle-base-services", "gradle-tooling-extension-impl", "gradle-tooling-api", "gradle", plugin = "gradle"))
    testCompile(ideaPluginDeps("Groovy", plugin = "Groovy"))
    testCompile(ideaSdkDeps("groovy-all"))

    testRuntime(project(":idea:idea-jvm"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":plugins:kapt3-idea"))
    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":plugins:lint"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))

    testRuntime(ideaSdkDeps("*.jar"))

    testRuntime(ideaPluginDeps("*.jar", plugin = "junit"))
    testRuntime(ideaPluginDeps("jcommander", "resources_en", plugin = "testng"))
    testRuntime(ideaPluginDeps("resources_en", plugin = "properties"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("jacocoant", plugin = "coverage"))
    //testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
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
}

configureInstrumentation()
