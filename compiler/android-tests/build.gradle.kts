
apply {
    plugin("kotlin")
}

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations

    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler.tests-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))

    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":core"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
    testCompile(ideaSdkDeps("jps-model.jar", subdir = "jps"))
    testCompile(ideaSdkDeps("groovy-all"))
    testCompile(ideaSdkDeps("openapi", "idea"))
    testCompile(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(ideaSdkDeps("jps-builders"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()


tasks.withType<Test> {
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    systemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}
