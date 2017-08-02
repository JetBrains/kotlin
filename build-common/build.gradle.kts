
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    compile(project(":core:util.runtime"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
    compile(ideaSdkDeps("util"))
    buildVersion()
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler.tests-common"))
    testCompile(protobufFull())
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Build Common")
    baseName = "kotlin-build-common"
}

testsJar {}

tasks.withType<Test> {
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    systemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

