apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    val testCompile by configurations
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-reflect"))
    compile(project(":compiler:backend"))
    compile(ideaSdkDeps("asm-all"))
//    compile(files(PathUtil.getJdkClassesRootsFromCurrentJre())) // TODO: make this one work instead of the nex one, since it contains more universal logic
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}
