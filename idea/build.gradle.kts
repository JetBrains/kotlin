import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compile(projectDist(":kotlin-stdlib-jre8"))
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
    compile(project(":kotlin-build-common"))
    compile(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compile(project(":kotlin-compiler-runner")) { isTransitive = false }
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

    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compile(commonDep("org.jetbrains", "markdown"))

    compileOnly(project(":kotlin-daemon-client"))

    compileOnly(intellijDep())
    compileOnly(commonDep("com.google.code.findbugs", "jsr305"))
    compileOnly(intellijPluginDep("IntelliLang"))
    compileOnly(intellijPluginDep("copyright"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java-i18n"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":idea:idea-jvm")) { isTransitive = false }
    testCompile(project(":idea:idea-gradle")) { isTransitive = false }
    testCompile(project(":idea:idea-maven")) { isTransitive = false }
    testCompile(commonDep("junit:junit"))

    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader"))

    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin")) { isTransitive = false }

    testRuntime(project(":plugins:android-extensions-compiler"))
    testRuntime(project(":plugins:android-extensions-ide")) { isTransitive = false }
    testRuntime(project(":allopen-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-allopen-compiler-plugin"))
    testRuntime(project(":noarg-ide-plugin")) { isTransitive = false }
    testRuntime(project(":kotlin-noarg-compiler-plugin"))
    testRuntime(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testRuntime(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testRuntime(project(":idea:idea-android")) { isTransitive = false }
    testRuntime(project(":plugins:lint")) { isTransitive = false }
    testRuntime(project(":plugins:uast-kotlin"))

    (rootProject.extra["compilerModules"] as Array<String>).forEach {
        testRuntime(project(it))
    }

    testCompile(intellijPluginDep("IntelliLang"))
    testCompile(intellijPluginDep("copyright"))
    testCompile(intellijPluginDep("properties"))
    testCompile(intellijPluginDep("java-i18n"))
    testCompileOnly(intellijDep())
    testCompileOnly(commonDep("com.google.code.findbugs", "jsr305"))
    testCompileOnly(intellijPluginDep("gradle"))
    testCompileOnly(intellijPluginDep("Groovy"))
    testCompileOnly(intellijPluginDep("maven"))

    testRuntime(intellijPluginDep("junit"))
    testRuntime(intellijPluginDep("gradle"))
    testRuntime(intellijPluginDep("Groovy"))
    testRuntime(intellijPluginDep("coverage"))
    testRuntime(intellijPluginDep("maven"))
    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijPluginDep("smali"))
    testRuntime(intellijPluginDep("testng"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs(
            "idea-completion/src",
            "idea-live-templates/src",
            "idea-repl/src"
        )
        resources.srcDirs("idea-repl/src").apply { include("META-INF/**") }
    }
    "test" {
        projectDefault()
        java.srcDirs(
            "idea-completion/tests",
            "idea-live-templates/tests"
        )
    }

}

val performanceTestCompile by configurations.creating {
    extendsFrom(configurations["testCompile"])
}

val performanceTestRuntime by configurations.creating {
    extendsFrom(configurations["testRuntime"])
}

val performanceTest by run {
    val sourceSets = javaPluginConvention().sourceSets
    sourceSets.creating {
        compileClasspath += sourceSets["test"].output
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output
        java.srcDirs("performanceTests")
    }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}


projectTest(taskName = "performanceTest") {
    dependsOn(":dist")
    dependsOn(performanceTest.output)
    testClassesDirs = performanceTest.output.classesDirs
    classpath = performanceTest.runtimeClasspath
    workingDir = rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")
    jvmArgs(
        "-XX:ReservedCodeCacheSize=240m",
        "-XX:+UseCompressedOops",
        "-XX:+UseConcMarkSweepGC"
    )
    jvmArgs("-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder")

    if (hasProperty("perf.flight.recorder.override")) {
        jvmArgs(property("perf.flight.recorder.override"))
    } else {
        val settings = if (hasProperty("perf.flight.recorder.settings")) ",settings=${property("perf.flight.recorder.settings")}" else ""
        jvmArgs("-XX:StartFlightRecording=delay=15m,duration=5h,filename=perf.jfr$settings")
    }

    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    }
}

testsJar {}

classesDirsArtifact()
configureInstrumentation()

