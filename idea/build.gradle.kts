import org.gradle.jvm.tasks.Jar
import org.jetbrains.intellij.IntelliJPluginExtension

apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("android", "copyright", "coverage", "gradle", "Groovy", "IntelliLang",
               "java-i18n", "junit", "maven", "properties", "testng")
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.serializer"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compile(project(":kotlin-compiler-runner"))
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
    compile(project(":kotlin-script-util")) { isTransitive = false }

    compile(preloadedDeps("markdown", "kotlinx-coroutines-core"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle")) { isTransitive = false }
    testCompile(project(":idea:idea-maven")) { isTransitive = false }
    testCompile(commonDep("junit:junit"))

    testCompileOnly(ideaPluginDeps("gradle-base-services", "gradle-tooling-extension-impl", "gradle-wrapper", plugin = "gradle"))
    testCompileOnly(ideaPluginDeps("Groovy", plugin = "Groovy"))
    testCompileOnly(ideaPluginDeps("maven", "maven-server-api", plugin = "maven"))

    testCompileOnly(ideaSdkDeps("groovy-all", "velocity", "gson", "jsr305", "idea_rt"))

    testRuntime(ideaSdkDeps("*.jar"))

    testRuntime(ideaPluginDeps("*.jar", plugin = "junit"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "properties"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "coverage"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "testng"))

    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader"))

    // deps below are test runtime deps, but made test compile to split compilation and running to reduce mem req
    testCompile(project(":plugins:android-extensions-compiler"))
    testCompile(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testCompile(project(":allopen-ide-plugin")) { isTransitive = false }
    testCompile(project(":kotlin-allopen-compiler-plugin"))
    testCompile(project(":noarg-ide-plugin")) { isTransitive = false }
    testCompile(project(":kotlin-noarg-compiler-plugin"))
    testCompile(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testCompile(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    testCompile(project(":idea:idea-android")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":plugins:uast-kotlin"))

    (rootProject.extra["compilerModules"] as Array<String>).forEach {
        testCompile(project(it))
    }
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellij { include("util.jar") })
        compileOnly(intellij {
            include("openapi.jar", "idea.jar", "velocity.jar", "boot.jar", "gson-*.jar",
                    "swingx-core-*.jar", "jsr305.jar", "forms_rt.jar", "util.jar", "annotations.jar")
        })
        compile(intellijPlugins("IntelliLang", "copyright", "properties", "java-i18n"))
        testCompileOnly(intellij { include("groovy-all-*.jar", "velocity.jar", "gson-*.jar", "jsr305.jar", "idea_rt.jar") })
        testCompileOnly(intellijPlugin("gradle") { include("gradle-base-services-*.jar", "gradle-tooling-extension-impl.jar", "gradle-wrapper-*.jar") })
        testCompileOnly(intellijPlugin("Groovy") { include("Groovy.jar") })
        testCompileOnly(intellijPlugin("maven") { include("maven.jar", "maven-server-api.jar") })
        testRuntime(intellij())
        testRuntime(intellijPlugins("junit", "gradle", "Groovy", "coverage", "maven", "android", "testng"))
    }
}

val processResources: Copy by tasks
processResources.from("../compiler/cli/src") {
    include("META-INF/extensions/compiler.xml")
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("idea-completion/src",
                     "idea-live-templates/src",
                     "idea-repl/src")
        resources.srcDirs("idea-repl/src").apply { include("META-INF/**") }
    }
    "test" {
        projectDefault()
        java.srcDirs(
                     "idea-completion/tests",
                     "idea-live-templates/tests")
    }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
    afterEvaluate {
        systemProperty("ideaSdk.path", the<IntelliJPluginExtension>().ideaDependency.classes.canonicalPath)
    }
}

testsJar {}

classesDirsArtifact()
configureInstrumentation()

