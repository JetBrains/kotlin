
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
    compile(ideaSdkCoreDeps("intellij-core"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testRuntime(ideaSdkCoreDeps("trove4j", "intellij-core"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

testsJar {}


tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}
