
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:cli-common"))
    compile(projectDist(":kotlin-daemon-client"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.serializer"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compile(project(":compiler:compiler-runner"))
    compile(project(":compiler:plugin-api"))
    compile(project(":eval4j"))
    compile(project(":j2k"))
    compile(project(":idea:formatter"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":idea:kotlin-gradle-tooling"))
    compile(project(":plugins:uast-kotlin"))
    compile(project(":plugins:uast-kotlin-idea"))

    compile(ideaSdkCoreDeps("intellij-core", "util"))

    compileOnly(ideaSdkDeps("openapi", "idea", "velocity", "boot", "gson", "swingx-core", "jsr305", "forms_rt"))

    compile(ideaPluginDeps("idea-junit", plugin = "junit"))
    compile(ideaPluginDeps("IntelliLang", plugin = "IntelliLang"))
    compile(ideaPluginDeps("testng", "testng-plugin", plugin = "testng"))
    compile(ideaPluginDeps("copyright", plugin = "copyright"))
    compile(ideaPluginDeps("properties", plugin = "properties"))
    compile(ideaPluginDeps("java-i18n", plugin = "java-i18n"))
    compile(ideaPluginDeps("coverage", plugin = "coverage"))
    compile(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))

    compileOnly(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
    compileOnly(ideaPluginDeps("Groovy", plugin = "Groovy"))
    compileOnly(ideaPluginDeps("maven", "maven-server-api", plugin = "maven"))

    compile(preloadedDeps("markdown", "kotlinx-coroutines-core"))

    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(commonDep("junit:junit"))

    testCompileOnly(ideaPluginDeps("gradle-base-services", "gradle-tooling-extension-impl", "gradle-wrapper", plugin = "gradle"))
    testCompileOnly(ideaPluginDeps("Groovy", plugin = "Groovy"))
    testCompileOnly(ideaPluginDeps("maven", "maven-server-api", plugin = "maven"))

    testCompileOnly(ideaSdkDeps("groovy-all", "velocity", "gson", "jsr305"))

    testRuntime(ideaSdkDeps("*.jar"))

    testRuntime(ideaPluginDeps("resources_en", plugin = "junit"))
    testRuntime(ideaPluginDeps("jcommander", "resources_en", plugin = "testng"))
    testRuntime(ideaPluginDeps("resources_en", plugin = "properties"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("jacocoant", plugin = "coverage"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))

    // deps below are test runtime deps, but made test compile to split compilation and running to reduce mem req
    testCompile(project(":android-extensions-compiler"))
    testCompile(project(":plugins:android-extensions-idea")) { isTransitive = false }
    testCompile(project(":plugins:allopen-ide")) { isTransitive = false }
    testCompile(project(":kotlin-allopen-compiler-plugin"))
    testCompile(project(":plugins:noarg-ide")) { isTransitive = false }
    testCompile(project(":kotlin-noarg-compiler-plugin"))
    testCompile(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testCompile(project(":plugins:sam-with-receiver-ide")) { isTransitive = false }
    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    testCompile(project(":idea:idea-android")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":plugins:uast-kotlin"))

    (rootProject.extra["compilerModules"] as Array<String>).forEach {
        testCompile(project(it))
    }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("idea-maven/src",
                     "idea-gradle/src",
                     "idea-completion/src",
                     "idea-live-templates/src",
                     "idea-repl/src")
        resources.srcDir("idea-maven/resources")
    }
    "test" {
        projectDefault()
        java.srcDirs("idea-maven/test",
                     "idea-completion/tests",
                     "idea-live-templates/tests")
    }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
}

testsJar {}
