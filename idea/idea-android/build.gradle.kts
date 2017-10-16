
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-reflect"))
    compile(project(":compiler:util"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-gradle"))

    compile(ideaSdkDeps("openapi", "idea"))
    compile(ideaPluginDeps("gradle-tooling-api", plugin = "gradle"))
    compile(ideaPluginDeps("android", "android-common", "sdklib", "sdk-common", "sdk-tools", "layoutlib-api", plugin = "android"))
    compile(preloadedDeps("dx", subdir = "android-5.0/lib"))

    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":compiler:tests-common"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm"))
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-gradle"))
    testCompile(ideaPluginDeps("properties", plugin = "properties"))
    testCompile(ideaSdkDeps("gson"))
    testCompile(commonDep("junit:junit"))

    testRuntime(project(":plugins:android-extensions-ide"))
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
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

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

testsJar {}

