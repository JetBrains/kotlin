
import java.io.File

apply { plugin("kotlin") }

jvmTarget = "1.6"

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

dependencies {
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    compileOnly(commonDep("org.fusesource.jansi", "jansi"))
    compileOnly(commonDep("org.jline", "jline"))

    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":compiler.tests-common"))
    testCompileOnly(project(":compiler:ir.ir2cfg"))
    testCompileOnly(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testCompile(ideaSdkDeps("openapi", "idea", "util", "asm-all", "commons-httpclient-3.1-patched"))
    // deps below are test runtime deps, but made test compile to split compilation and running to reduce mem req
    testCompile(project(":kotlin-stdlib"))
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-runtime"))
    testCompile(project(":kotlin-reflect"))
    testCompile(project(":android-extensions-compiler"))
    testCompile(project(":kotlin-ant"))
    otherCompilerModules.forEach {
        testCompile(project(it))
    }
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
//    testRuntime(project(":kotlin-compiler", configuration = "default"))
}

sourceSets {
    "main" {
        java.srcDirs("daemon/src",
                     "conditional-preprocessor/src")
        resources.srcDir("../idea/src").apply {
            include("META-INF/extensions/common.xml",
                    "META-INF/extensions/kotlin2jvm.xml",
                    "META-INF/extensions/kotlin2js.xml")
        }
    }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
}

