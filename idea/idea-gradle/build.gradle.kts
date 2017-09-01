apply { plugin("kotlin") }

dependencies {

    compileOnly(ideaSdkDeps("openapi", "idea"))
    compileOnly(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
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
    testCompile(ideaSdkDeps("openapi", "idea", "groovy-all"))
    testCompile(ideaPluginDeps("gradle-wrapper", "gradle-base-services", "gradle-tooling-extension-impl", "gradle-tooling-api", "gradle", plugin = "gradle"))
    testCompile(ideaPluginDeps("Groovy", plugin = "Groovy"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    workingDir = rootDir
}