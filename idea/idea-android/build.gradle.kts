apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations

    compile(project(":kotlin-reflect"))
    compile(project(":compiler:util"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:ide-common"))
    compile(ideaSdkDeps("openapi", "idea"))
    compile(ideaPluginDeps("gradle-tooling-api", plugin = "gradle"))
    compile(ideaPluginDeps("android", "common", "sdklib", "sdk-common", "layoutlib-api", plugin = "android"))
    compile(preloadedDeps("uast-common", "uast-java"))
    compile(preloadedDeps("dx", subdir = "android-5.0/lib"))

    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(projectTests(":idea"))
    testCompile(ideaPluginDeps("android-common", "sdklib", plugin = "android"))
    testCompile(ideaPluginDeps("properties", plugin = "properties"))
    testCompile(ideaSdkDeps("gson"))

    testRuntime(preloadedDeps("uast-common", "uast-java"))
    testRuntime(project(":plugins:android-extensions-idea"))
    testRuntime(project(":plugins:sam-with-receiver-ide"))
    testRuntime(project(":plugins:noarg-ide"))
    testRuntime(project(":plugins:allopen-ide"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaPluginDeps("idea-junit", "resources_en", plugin = "junit"))
    testRuntime(ideaPluginDeps("IntelliLang", plugin = "IntelliLang"))
    testRuntime(ideaPluginDeps("jcommander", "testng", "testng-plugin", "resources_en", plugin = "testng"))
    testRuntime(ideaPluginDeps("copyright", plugin = "copyright"))
    testRuntime(ideaPluginDeps("properties", "resources_en", plugin = "properties"))
    testRuntime(ideaPluginDeps("java-i18n", plugin = "java-i18n"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("coverage", "jacocoant", plugin = "coverage"))
    testRuntime(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

tasks.withType<Test> {
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

testsJar {}

