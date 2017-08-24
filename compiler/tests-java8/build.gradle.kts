import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply { plugin("kotlin") }

dependencies {
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":core"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:serialization"))
    testCompile(ideaSdkDeps("openapi", "idea", "util", "asm-all"))
    // deps below are test runtime deps, but made test compile to split compilation and running to reduce mem req
    testCompile(projectDist(":kotlin-stdlib"))
    testCompile(projectDist(":kotlin-script-runtime"))
    testCompile(projectDist(":kotlin-runtime"))
    testCompile(projectDist(":kotlin-reflect"))
    testCompile(projectTests(":compiler"))
    testRuntime(projectDist(":kotlin-preloader"))
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jdkHome = rootProject.extra["JDK_18"]!!.toString()
    kotlinOptions.jvmTarget = "1.8"
}

testsJar {}

projectTest {
    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
}

