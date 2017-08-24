
import java.io.File
import org.gradle.api.tasks.bundling.Jar

apply { plugin("kotlin") }

jvmTarget = "1.6"

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

val depDistProjects = listOf(
        ":kotlin-script-runtime",
        ":kotlin-stdlib",
        ":kotlin-reflect",
        ":kotlin-test:kotlin-test-jvm")

// TODO: it seems incomplete, find and add missing dependencies
val testDistProjects = listOf(
        "", // for root project
        ":prepare:mock-runtime-for-test",
        ":kotlin-compiler",
        ":kotlin-runtime",
        ":kotlin-script-runtime",
        ":kotlin-stdlib",
        ":kotlin-stdlib-jre7",
        ":kotlin-stdlib-jre8",
        ":kotlin-stdlib-js",
        ":kotlin-reflect",
        ":kotlin-test:kotlin-test-jvm",
        ":kotlin-test:kotlin-test-junit",
        ":kotlin-test:kotlin-test-js",
        ":kotlin-daemon-client",
        ":android-extensions-compiler",
        ":kotlin-ant")

dependencies {
    depDistProjects.forEach {
        testCompile(projectDist(it))
    }
    testCompile(commonDep("junit:junit"))
    testCompileOnly(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":compiler:ir.ir2cfg"))
    testCompile(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    otherCompilerModules.forEach {
        testCompileOnly(project(it))
    }
    testCompile(ideaSdkDeps("openapi", "idea", "util", "asm-all", "commons-httpclient-3.1-patched"))
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
        // not yet ready
//        java.srcDir("tests-ir-jvm/tests")
    }
}

val jar: Jar by tasks
jar.apply {
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    from("../idea/src").apply {
        include("META-INF/extensions/common.xml",
                "META-INF/extensions/kotlin2jvm.xml",
                "META-INF/extensions/kotlin2js.xml")
    }
}

testsJar {}

projectTest {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
}

