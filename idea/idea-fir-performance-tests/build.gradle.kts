plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":idea:idea-frontend-fir"))
    compile(project(":idea:formatter"))
    compile(intellijDep())
    compile(intellijCoreDep())

// <temp>
    compile(project(":idea:idea-core"))
    compile(project(":idea"))
// </temp>
    testCompile(projectTests(":idea:performanceTests"))


    testCompile(toolsJar())
    testCompile(projectTests(":idea"))
    testCompile(projectTests(":idea:idea-fir"))
    compile(project(":idea:idea-fir"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":idea:idea-test-framework"))
    testCompile(projectTests(":idea:idea-frontend-fir"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))

    testCompileOnly(intellijDep())
    testRuntime(intellijDep())

    compile(intellijPluginDep("java"))
    testRuntimeOnly(intellijPluginDep("java"))

    testRuntimeOnly(intellijDep())
    testRuntimeOnly(intellijRuntimeAnnotations())
    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":plugins:android-extensions-ide"))
    testRuntimeOnly(project(":plugins:kapt3-idea"))
    testRuntimeOnly(project(":sam-with-receiver-ide-plugin"))
    testRuntimeOnly(project(":noarg-ide-plugin"))
    testRuntimeOnly(project(":allopen-ide-plugin"))
    testRuntimeOnly(project(":kotlin-scripting-idea"))
    testRuntimeOnly(project(":kotlinx-serialization-ide-plugin"))
    testRuntimeOnly(project(":plugins:parcelize:parcelize-ide"))
    testRuntimeOnly(project(":plugins:lombok:lombok-ide-plugin"))
    testRuntimeOnly(project(":nj2k:nj2k-services"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":idea:kotlin-gradle-tooling"))
    testRuntimeOnly(project(":kotlin-gradle-statistics"))

    testImplementation("khttp:khttp:1.0.0")

    testImplementation(intellijPluginDep("gradle-java"))
    testRuntimeOnly(intellijPluginDep("gradle-java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = project.rootDir
    val useFirIdeaPlugin = kotlinBuildProperties.useFirIdeaPlugin
    doFirst {
        if (!useFirIdeaPlugin) {
            error("Test task in the module should be executed with -Pidea.fir.plugin=true")
        }
    }
}

testsJar()

projectTest(taskName = "ideaFirPerformanceTest") {
    exclude("**/*WholeProjectPerformanceComparisonFirImplTest*")
    val currentOs = org.gradle.internal.os.OperatingSystem.current()

    if (!currentOs.isWindows) {
        project.providers.systemProperty("ASYNC_PROFILER_HOME").forUseAtConfigurationTime().orNull?.let { asyncProfilerHome ->
            classpath += project.files("$asyncProfilerHome/build/async-profiler.jar")
        }
    }

    workingDir = project.rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-Didea.debug.mode=true")
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")

    jvmArgs(
        "-XX:+UseCompressedOops",
        "-Didea.ProcessCanceledException=disabled",
        "-XX:+UseConcMarkSweepGC"
    )

    project.providers.systemProperty("YOURKIT_PROFILER_HOME").forUseAtConfigurationTime().orNull?.let {yourKitHome ->
        when {
            currentOs.isLinux -> {
                jvmArgs("-agentpath:$yourKitHome/bin/linux-x86-64/libyjpagent.so")
                classpath += files("$yourKitHome/lib/yjp-controller-api-redist.jar")
            }
            currentOs.isMacOsX -> {
                jvmArgs("-agentpath:$yourKitHome/Contents/Resources/bin/mac/libyjpagent.dylib=delay=5000,_socket_timeout_ms=120000,disablealloc,disable_async_sampling,disablenatives")
                classpath += files("$yourKitHome/Contents/Resources/lib/yjp-controller-api-redist.jar")
            }
        }
    }

    systemProperty("idea.home.path", project.intellijRootDir().canonicalPath)

    project.providers.gradleProperty("cacheRedirectorEnabled").forUseAtConfigurationTime().orNull?.let {
        systemProperty("kotlin.test.gradle.import.arguments", "-PcacheRedirectorEnabled=$it")
    }
}

projectTest(taskName = "firProjectPerformanceTest") {
     include("**/*WholeProjectPerformanceComparisonFirImplTest*")

    workingDir = rootDir

    jvmArgs?.removeAll { it.startsWith("-Xmx") }

    maxHeapSize = "3g"
    jvmArgs("-DperformanceProjects=${System.getProperty("performanceProjects")}")
    jvmArgs("-Didea.debug.mode=true")
    jvmArgs("-DemptyProfile=${System.getProperty("emptyProfile")}")
    jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=50")
    jvmArgs(
        "-XX:+UseCompressedOops",
        "-XX:+UseConcMarkSweepGC"
    )

    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
        project.findProperty("cacheRedirectorEnabled")?.let {
            systemProperty("kotlin.test.gradle.import.arguments", "-PcacheRedirectorEnabled=$it")
        }
    }
}
