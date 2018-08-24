
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import java.util.*
import java.io.File
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import proguard.gradle.ProGuardTask

buildscript {
    extra["defaultSnapshotVersion"] = "1.2-SNAPSHOT"

    kotlinBootstrapFrom(BootstrapOption.TeamCity("1.2.70-dev-491", onlySuccessBootstrap = false))

    repositories {
        bootstrapKotlinRepo?.let(::maven)
        maven("https://plugins.gradle.org/m2")
    }

    // a workaround for kotlin compiler classpath in kotlin project: sometimes gradle substitutes
    // kotlin-stdlib external dependency with local project :kotlin-stdlib in kotlinCompilerClasspath configuration.
    // see also configureCompilerClasspath@
    val bootstrapCompilerClasspath by configurations.creating

    dependencies {
        bootstrapCompilerClasspath(kotlin("compiler-embeddable", bootstrapKotlinVersion))

        classpath("com.gradle.publish:plugin-publish-plugin:0.9.7")
        classpath(kotlin("gradle-plugin", bootstrapKotlinVersion))
        classpath("net.sf.proguard:proguard-gradle:5.3.3")
    }
}

plugins {
    `build-scan`
    idea
    id("jps-compatible")
}

pill {
    excludedDirs(
        "out",
        "buildSrc/build",
        "buildSrc/prepare-deps/android-dx/build",
        "buildSrc/prepare-deps/intellij-sdk/build"
    )
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
}

val configuredJdks: List<JdkId> =
        getConfiguredJdks().also {
            it.forEach {
                logger.info("Using ${it.majorVersion} home: ${it.homeDir}")
            }
        }

val defaultSnapshotVersion: String by extra
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(findProperty("deployVersion")?.toString() ?: buildNumber)

val kotlinLanguageVersion by extra("1.2")

allprojects {
    group = "org.jetbrains.kotlin"
    version = kotlinVersion
}

extra["kotlin_root"] = rootDir

val cidrKotlinPlugin by configurations.creating

dependencies {
    cidrKotlinPlugin(project(":prepare:cidr-plugin", "runtimeJar"))
}

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val ideaUltimateSandboxDir = "$commonLocalDataDir/ideaUltimateSandbox"
val ideaPluginDir = "$distDir/artifacts/ideaPlugin/Kotlin"
val ideaUltimatePluginDir = "$distDir/artifacts/ideaUltimatePlugin/Kotlin"
val cidrPluginDir = "$distDir/artifacts/cidrPlugin/Kotlin"

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaUltimateSandboxDir"] = project.file(ideaUltimateSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["ideaUltimatePluginDir"] = project.file(ideaUltimatePluginDir)
extra["cidrPluginDir"] = project.file(cidrPluginDir)
extra["isSonatypeRelease"] = false

extra["JDK_16"] = jdkPath("1.6")
extra["JDK_17"] = jdkPath("1.7")
extra["JDK_18"] = jdkPath("1.8")
extra["JDK_9"] = jdkPath("9")
extra["JDK_10"] = jdkPathIfFound("10")

rootProject.apply {
    from(rootProject.file("versions.gradle.kts"))
    from(rootProject.file("report.gradle.kts"))
}

extra["versions.protobuf-java"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.jansi"] = "1.16"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"
extra["versions.android"] = "2.3.1"
extra["versions.kotlinx-coroutines-core"] = "0.20"
extra["versions.kotlinx-coroutines-jdk8"] = "0.20"
extra["versions.json"] = "20160807"
extra["versions.native-platform"] = "0.14"
extra["versions.ant-launcher"] = "1.8.0"
extra["versions.robolectric"] = "3.1"
extra["versions.org.springframework"] = "4.2.0.RELEASE"
extra["versions.jflex"] = "1.7.0"
extra["versions.markdown"] = "0.1.25"

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null
val intellijUltimateEnabled = project.getBooleanProperty("intellijUltimateEnabled") ?: isTeamcityBuild
val effectSystemEnabled by extra(project.getBooleanProperty("kotlin.compiler.effectSystemEnabled") ?: false)
val newInferenceEnabled by extra(project.getBooleanProperty("kotlin.compiler.newInferenceEnabled") ?: false)

val intellijSeparateSdks = project.getBooleanProperty("intellijSeparateSdks") ?: false

extra["intellijUltimateEnabled"] = intellijUltimateEnabled
extra["intellijSeparateSdks"] = intellijSeparateSdks

extra["IntellijCoreDependencies"] =
        listOf("annotations",
               "asm-all",
               "guava",
               "jdom",
               "jna",
               "log4j",
               "picocontainer",
               "snappy-in-java",
               "streamex",
               "trove4j")


extra["compilerModules"] = arrayOf(
        ":compiler:util",
        ":compiler:container",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:psi",
        ":compiler:frontend",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":compiler:cli-common",
        ":compiler:daemon-common",
        ":compiler:daemon",
        ":compiler:ir.tree",
        ":compiler:ir.psi2ir",
        ":compiler:ir.backend.common",
        ":compiler:backend.js",
        ":compiler:backend-common",
        ":compiler:backend",
        ":compiler:plugin-api",
        ":compiler:light-classes",
        ":compiler:cli",
        ":compiler:incremental-compilation-impl",
        ":js:js.ast",
        ":js:js.serializer",
        ":js:js.parser",
        ":js:js.frontend",
        ":js:js.translator",
        ":js:js.dce",
        ":compiler",
        ":kotlin-build-common",
        ":core:metadata",
        ":core:metadata.jvm",
        ":core:descriptors",
        ":core:descriptors.jvm",
        ":core:deserialization",
        ":core:util.runtime"
)

val coreLibProjects = listOf(
        ":kotlin-stdlib",
        ":kotlin-stdlib-common",
        ":kotlin-stdlib-js",
        ":kotlin-stdlib-jre7",
        ":kotlin-stdlib-jre8",
        ":kotlin-stdlib-jdk7",
        ":kotlin-stdlib-jdk8",
        ":kotlin-test:kotlin-test-common",
        ":kotlin-test:kotlin-test-jvm",
        ":kotlin-test:kotlin-test-junit",
        ":kotlin-test:kotlin-test-junit5",
        ":kotlin-test:kotlin-test-testng",
        ":kotlin-test:kotlin-test-js",
        ":kotlin-reflect"
)

val gradlePluginProjects = listOf(
        ":kotlin-gradle-plugin",
        ":kotlin-gradle-plugin:plugin-marker",
        ":kotlin-gradle-plugin-api",
//        ":kotlin-gradle-plugin-integration-tests",  // TODO: build fails
        ":kotlin-allopen",
        ":kotlin-allopen:plugin-marker",
        ":kotlin-annotation-processing-gradle",
        ":kotlin-noarg",
        ":kotlin-noarg:plugin-marker",
        ":kotlin-sam-with-receiver"
)

apply {
    from("libraries/commonConfiguration.gradle")
    from("libraries/configureGradleTools.gradle")
}

apply {
    if (extra["isSonatypeRelease"] as? Boolean == true) {
        logger.info("Applying configuration for sonatype release")
        from("libraries/prepareSonatypeStaging.gradle")
    }
}

fun Task.listConfigurationContents(configName: String) {
    doFirst {
        project.configurations.findByName(configName)?.let {
            println("$configName configuration files:\n${it.allArtifacts.files.files.joinToString("\n  ", "  ")}")
        }
    }
}

val defaultJvmTarget = "1.8"
val defaultJavaHome = jdkPath(defaultJvmTarget!!)
val ignoreTestFailures by extra(project.findProperty("ignoreTestFailures")?.toString()?.toBoolean() ?: project.hasProperty("teamcity"))

allprojects {

    jvmTarget = defaultJvmTarget
    javaHome = defaultJavaHome

    // There are problems with common build dir:
    //  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
    //  - idea seems unable to exclude common builddir from indexing
    // therefore it is disabled by default
    // buildDir = File(commonBuildDir, project.name)

    val mirrorRepo: String? = findProperty("maven.repository.mirror")?.toString()

    repositories {
        intellijSdkRepo(project)
        androidDxJarRepo(project)
        mirrorRepo?.let(::maven)
        bootstrapKotlinRepo?.let(::maven)
        jcenter()
    }

    configureJvmProject(javaHome!!, jvmTarget!!)

    val commonCompilerArgs = listOfNotNull(
        "-Xallow-kotlin-package",
        "-Xread-deserialized-contracts",
        "-Xprogressive".takeIf { hasProperty("test.progressive.mode") }
    )

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            languageVersion = kotlinLanguageVersion
            apiVersion = kotlinLanguageVersion
            freeCompilerArgs = commonCompilerArgs
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
        kotlinOptions {
            freeCompilerArgs = commonCompilerArgs + listOf("-Xnormalize-constructor-calls=enable")
        }
    }

    tasks.withType(VerificationTask::class.java as Class<Task>) {
        (this as VerificationTask).ignoreFailures = ignoreTestFailures
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    task<Jar>("javadocJar") {
        classifier = "javadoc"
    }

    tasks.withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    task("listArchives") { listConfigurationContents("archives") }

    task("listRuntimeJar") { listConfigurationContents("runtimeJar") }

    task("listDistJar") { listConfigurationContents("distJar") }

    afterEvaluate {
        logger.info("configuring project $name to compile to the target jvm version $jvmTarget using jdk: $javaHome")
        if (javaHome != defaultJavaHome || jvmTarget != defaultJvmTarget) {
            configureJvmProject(javaHome!!, jvmTarget!!)
        }

        fun File.toProjectRootRelativePathOrSelf() = (relativeToOrNull(rootDir)?.takeUnless { it.startsWith("..") } ?: this).path

        fun FileCollection.printClassPath(role: String) =
                println("${project.path} $role classpath:\n  ${joinToString("\n  ") { it.toProjectRootRelativePathOrSelf() } }")

        try { javaPluginConvention() } catch (_: UnknownDomainObjectException) { null }?.let { javaConvention ->
            task("printCompileClasspath") { doFirst { javaConvention.sourceSets["main"].compileClasspath.printClassPath("compile") } }
            task("printRuntimeClasspath") { doFirst { javaConvention.sourceSets["main"].runtimeClasspath.printClassPath("runtime") } }
            task("printTestCompileClasspath") { doFirst { javaConvention.sourceSets["test"].compileClasspath.printClassPath("test compile") } }
            task("printTestRuntimeClasspath") { doFirst { javaConvention.sourceSets["test"].runtimeClasspath.printClassPath("test runtime") } }
        }

        run configureCompilerClasspath@ {
            val bootstrapCompilerClasspath by rootProject.buildscript.configurations
            configurations.findByName("kotlinCompilerClasspath")?.let {
                dependencies.add(it.name, files(bootstrapCompilerClasspath))
            }
        }
    }
}

gradle.taskGraph.whenReady {
    if (isTeamcityBuild) {
        logger.warn("CI build profile is active (IC is off, proguard is on). Use -Pteamcity=false to reproduce local build")
        for (task in allTasks) {
            when (task) {
                is AbstractKotlinCompile<*> -> task.incremental = false
                is JavaCompile -> task.options.isIncremental = false
            }
        }
    } else {
        logger.warn("Local build profile is active (IC is on, proguard is off). Use -Pteamcity=true to reproduce TC build")
        for (task in allTasks) {
            when (task) {
                // todo: remove when Gradle 4.10+ is used (Java IC on by default)
                is JavaCompile -> task.options.isIncremental = true
                is org.gradle.jvm.tasks.Jar -> task.entryCompression = ZipEntryCompression.STORED
            }
        }
    }
}

val dist by task<Copy> {
    val childDistTasks = getTasksByName("dist", true) - this@task
    dependsOn(childDistTasks)

    into(distDir)
    from(files("compiler/cli/bin")) { into("kotlinc/bin") }
    from(files("license")) { into("kotlinc/license") }
}

val copyCompilerToIdeaPlugin by task<Copy> {
    dependsOn(dist)
    into(ideaPluginDir)
    from(distDir) { include("kotlinc/**") }
}

val ideaPlugin by task<Task> {
    dependsOn(copyCompilerToIdeaPlugin)
    val childIdeaPluginTasks = getTasksByName("ideaPlugin", true) - this@task
    dependsOn(childIdeaPluginTasks)
}

tasks {
    "clean" {
        doLast {
            delete("$buildDir/repo")
            delete(distDir)
        }
    }

    "cleanupArtifacts" {
        doLast {
            delete(ideaPluginDir)
            delete(ideaUltimatePluginDir)
            delete(cidrPluginDir)
        }
    }

    "coreLibsTest" {
        (coreLibProjects + listOf(
                ":kotlin-stdlib:samples",
                ":kotlin-test:kotlin-test-js:kotlin-test-js-it",
                ":kotlinx-metadata-jvm",
                ":tools:binary-compatibility-validator"
        )).forEach {
            dependsOn(it + ":check")
        }
    }

    "gradlePluginTest" {
        gradlePluginProjects.forEach {
            dependsOn(it + ":check")
        }
    }

    "gradlePluginIntegrationTest" {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    "jvmCompilerTest" {
        dependsOn("dist")
        dependsOn(":compiler:test",
                  ":compiler:container:test",
                  ":compiler:tests-java8:test")
    }

    "jsCompilerTest" {
        dependsOn(":js:js.tests:test")
        dependsOn(":js:js.tests:runMocha")
    }

    "scriptingTest" {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
    }

    "compilerTest" {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")

        dependsOn("scriptingTest")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":compiler:incremental-compilation-impl:test")
    }

    "toolsTest" {
        dependsOn(":tools:kotlinp:test")
    }

    "examplesTest" {
        dependsOn("dist")
        (project(":examples").subprojects + project(":kotlin-gradle-subplugin-example")).forEach { p ->
            dependsOn("${p.path}:check")
        }
    }

    "distTest" {
        dependsOn("compilerTest")
        dependsOn("toolsTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }

    "specTest" {
        dependsOn("dist")
        dependsOn(":compiler:tests-spec:test")
    }

    "androidCodegenTest" {
        dependsOn(":compiler:android-tests:test")
    }

    "jps-tests" {
        dependsOn("dist")
        dependsOn(":jps-plugin:test")
    }

    "idea-plugin-main-tests" {
        dependsOn("dist")
        dependsOn(":idea:test")
    }

    "idea-plugin-additional-tests" {
        dependsOn("dist")
        dependsOn(":idea:idea-gradle:test",
                  ":idea:idea-maven:test",
                  ":j2k:test",
                  ":eval4j:test")
    }

    "idea-plugin-tests" {
        dependsOn("dist")
        dependsOn("idea-plugin-main-tests",
                  "idea-plugin-additional-tests")
    }

    "android-ide-tests" {
        dependsOn("dist")
        dependsOn(":plugins:android-extensions-ide:test",
                  ":idea:idea-android:test",
                  ":kotlin-annotation-processing:test")
    }

    "plugins-tests" {
        dependsOn("dist")
        dependsOn(":kotlin-annotation-processing:test",
                  ":kotlin-source-sections-compiler-plugin:test",
                  ":kotlin-allopen-compiler-plugin:test",
                  ":kotlin-noarg-compiler-plugin:test",
                  ":kotlin-sam-with-receiver-compiler-plugin:test",
                  ":plugins:uast-kotlin:test",
                  ":kotlin-annotation-processing-gradle:test")
    }


    "ideaPluginTest" {
        dependsOn(
                "idea-plugin-tests",
                "jps-tests",
                "plugins-tests",
                "android-ide-tests",
                ":generators:test"
        )
    }


    "test" {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }
    "check" { dependsOn("test") }
}

fun CopySpec.setExecutablePermissions() {
    filesMatching("**/bin/*") { mode = 0b111101101 }
    filesMatching("**/bin/*.bat") { mode = 0b110100100 }
}

val zipCompiler by task<Zip> {
    dependsOn(dist)
    destinationDir = file(distDir)
    archiveName = "kotlin-compiler-$kotlinVersion.zip"

    from(distKotlinHomeDir)
    into("kotlinc")
    setExecutablePermissions()

    doLast {
        logger.lifecycle("Compiler artifacts packed to $archivePath")
    }
}

val zipTestData by task<Zip> {
    destinationDir = file(distDir)
    archiveName = "kotlin-test-data.zip"
    from("compiler/testData") { into("compiler") }
    from("idea/testData") { into("ide") }
    from("idea/idea-completion/testData") { into("ide/completion") }
    doLast {
        logger.lifecycle("Test data packed to $archivePath")
    }
}

val zipPlugin by task<Zip> {
    val src = when (project.findProperty("pluginArtifactDir") as String?) {
        "Kotlin" -> ideaPluginDir
        "KotlinUltimate" -> ideaUltimatePluginDir
        null -> if (project.hasProperty("ultimate")) ideaUltimatePluginDir else ideaPluginDir
        else -> error("Unsupported plugin artifact dir")
    }
    val destPath = project.findProperty("pluginZipPath") as String?
    val dest = File(destPath ?: "$buildDir/kotlin-plugin.zip")
    destinationDir = dest.parentFile
    archiveName = dest.name
    doFirst {
        if (destPath == null) throw GradleException("Specify target zip path with 'pluginZipPath' property")
    }

    from(src)
    into("Kotlin")
    setExecutablePermissions()

    doLast {
        logger.lifecycle("Plugin artifacts packed to $archivePath")
    }
}

val cidrPlugin by task<Copy> {
    dependsOn(ideaPlugin)
    into(cidrPluginDir)
    from(ideaPluginDir) {
        exclude("lib/kotlin-plugin.jar")

        exclude("lib/android-lint.jar")
        exclude("lib/android-ide.jar")
        exclude("lib/android-output-parser-ide.jar")
        exclude("lib/android-extensions-ide.jar")
        exclude("lib/android-extensions-compiler.jar")
        exclude("lib/kapt3-idea.jar")
        exclude("lib/jps-ide.jar")
        exclude("lib/jps/**")
        exclude("kotlinc/**")
        exclude("lib/maven-ide.jar")
    }
    from(cidrKotlinPlugin) { into("lib") }
}

val zipCidrPlugin by task<Zip> {
    val destPath = project.findProperty("pluginZipPath") as String?
            ?: "$distDir/artifacts/kotlin-plugin-$kotlinVersion-CIDR.zip"
    val destFile = File(destPath)

    destinationDir = destFile.parentFile
    archiveName = destFile.name

    from(cidrPlugin)
    into("Kotlin")
    setExecutablePermissions()

    doLast {
        logger.lifecycle("Plugin artifacts packed to $archivePath")
    }
}

configure<IdeaModel> {
    module {
        excludeDirs = files(
                project.buildDir,
                commonLocalDataDir,
                ".gradle",
                "dependencies",
                "dist"
        ).toSet()
    }
}

fun jdkPathIfFound(version: String): String? {
    val jdkName = "JDK_${version.replace(".", "")}"
    val jdkMajorVersion = JdkMajorVersion.valueOf(jdkName)
    return configuredJdks.find { it.majorVersion == jdkMajorVersion }?.homeDir?.canonicalPath
}

fun jdkPath(version: String): String = jdkPathIfFound(version)
        ?: throw GradleException ("Please set environment variable JDK_${version.replace(".", "")} to point to JDK $version installation")

fun Project.configureJvmProject(javaHome: String, javaVersion: String) {
    tasks.withType<JavaCompile> {
        if (name != "compileJava9Java") {
            options.isFork = true
            options.forkOptions.javaHome = file(javaHome)
            options.compilerArgs.add("-proc:none")
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jdkHome = javaHome
        kotlinOptions.jvmTarget = javaVersion
    }

    tasks.withType<Test> {
        executable = File(javaHome, "bin/java").canonicalPath
    }
}

tasks.create("findShadowJarsInClasspath").doLast {
    fun Collection<File>.printSorted(indent: String = "    ") {
        sortedBy { it.path }.forEach { println(indent + it.relativeTo(rootProject.projectDir)) }
    }

    val shadowJars = hashSetOf<File>()
    for (project in rootProject.allprojects) {
        for (task in project.tasks) {
            when (task) {
                is ShadowJar -> {
                    shadowJars.add(fileFrom(task.archivePath))
                }
                is ProGuardTask -> {
                    shadowJars.addAll(task.outputs.files.toList())
                }
            }
        }
    }

    println("Shadow jars:")
    shadowJars.printSorted()

    fun Project.checkConfig(configName: String) {
        val config = configurations.findByName(configName) ?: return
        val shadowJarsInConfig = config.resolvedConfiguration.files.filter { it in shadowJars }
        if (shadowJarsInConfig.isNotEmpty()) {
            println()
            println("Project $project contains shadow jars in configuration '$configName':")
            shadowJarsInConfig.printSorted()
        }
    }

    for (project in rootProject.allprojects) {
        project.checkConfig("compileClasspath")
        project.checkConfig("testCompileClasspath")
    }
}
