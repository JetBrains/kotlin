
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask

buildscript {
    extra["defaultSnapshotVersion"] = "1.3-SNAPSHOT"

    kotlinBootstrapFrom(BootstrapOption.TeamCity("1.3.20-dev-1708", onlySuccessBootstrap = false))

    repositories.withRedirector(project) {
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
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.17")
    }
}

plugins {
    `build-scan` version "1.15"
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

val kotlinLanguageVersion by extra("1.3")

allprojects {
    group = "org.jetbrains.kotlin"
    version = kotlinVersion
}

extra["kotlin_root"] = rootDir

val cidrKotlinPlugin by configurations.creating
val appcodeKotlinPlugin by configurations.creating
val clionKotlinPlugin by configurations.creating

val includeCidr by extra(project.getBooleanProperty("cidrPluginsEnabled") ?: false)

dependencies {
    if (includeCidr) {
        cidrKotlinPlugin(project(":prepare:cidr-plugin", "runtimeJar"))
        appcodeKotlinPlugin(project(":prepare:appcode-plugin", "runtimeJar"))
        clionKotlinPlugin(project(":prepare:clion-plugin", "runtimeJar"))
    }
}

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val ideaUltimateSandboxDir = "$commonLocalDataDir/ideaUltimateSandbox"
val clionSandboxDir = "$commonLocalDataDir/clionSandbox"
val appcodeSandboxDir = "$commonLocalDataDir/appcodeSandbox"
val ideaPluginDir = "$distDir/artifacts/ideaPlugin/Kotlin"
val ideaUltimatePluginDir = "$distDir/artifacts/ideaUltimatePlugin/Kotlin"
val cidrPluginDir = "$distDir/artifacts/cidrPlugin/Kotlin"
val appcodePluginDir = "$distDir/artifacts/appcodePlugin/Kotlin"
val clionPluginDir = "$distDir/artifacts/clionPlugin/Kotlin"

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaUltimateSandboxDir"] = project.file(ideaUltimateSandboxDir)
extra["clionSandboxDir"] = project.file(ideaSandboxDir)
extra["appcodeSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["ideaUltimatePluginDir"] = project.file(ideaUltimatePluginDir)
extra["cidrPluginDir"] = project.file(cidrPluginDir)
extra["appcodePluginDir"] = project.file(appcodePluginDir)
extra["clionPluginDir"] = project.file(clionPluginDir)
extra["isSonatypeRelease"] = false

// Work-around necessary to avoid setting null javaHome. Will be removed after support of lazy task configuration
val jdkNotFoundConst = "JDK NOT FOUND"

extra["JDK_16"] = jdkPath("1.6")
extra["JDK_17"] = jdkPath("1.7")
extra["JDK_18"] = jdkPath("1.8")
extra["JDK_9"] = jdkPath("9")
extra["JDK_10"] = jdkPath("10")
extra["JDK_11"] = jdkPath("11")

gradle.taskGraph.beforeTask() {
    checkJDK()
}

var jdkChecked: Boolean = false
fun checkJDK() {
    if (jdkChecked) {
        return
    }
    var unpresentJdks = JdkMajorVersion.values().filter { it.isMandatory() }.map { it -> it.name }.filter { it == null || extra[it] == jdkNotFoundConst }.toList()
    if (!unpresentJdks.isEmpty()) {
        throw GradleException("Please set environment variable${if (unpresentJdks.size > 1) "s" else ""}: ${unpresentJdks.joinToString()} to point to corresponding JDK installation.")
    }
    jdkChecked = true
}

rootProject.apply {
    from(rootProject.file("versions.gradle.kts"))
    from(rootProject.file("report.gradle.kts"))
    from(rootProject.file("javaInstrumentation.gradle.kts"))
}

IdeVersionConfigurator.setCurrentIde(this)

extra["versions.protobuf-java"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.jansi"] = "1.16"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"
extra["versions.android"] = "2.3.1"
extra["versions.kotlinx-coroutines-core"] = "1.0.1"
extra["versions.kotlinx-coroutines-jdk8"] = "1.0.1"
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
               if (Platform[191].orHigher()) "asm-all-7.0" else "asm-all",
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
        *if (project.findProperty("fir.enabled") == "true") {
            arrayOf(
                ":compiler:fir:cones",
                ":compiler:fir:resolve",
                ":compiler:fir:tree",
                ":compiler:fir:psi2fir"
            )
        } else {
            emptyArray()
        },
        ":compiler:frontend",
        ":compiler:frontend.common",
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
val defaultJavaHome = jdkPath(defaultJvmTarget)
val ignoreTestFailures by extra(project.findProperty("ignoreTestFailures")?.toString()?.toBoolean() ?: project.hasProperty("teamcity"))

allprojects {

    jvmTarget = defaultJvmTarget
    if (defaultJavaHome != null) {
        javaHome = defaultJavaHome
    } else {
        logger.error("Could not find default java home. Please set environment variable JDK_${defaultJavaHome} to point to JDK ${defaultJavaHome} installation.")
    }


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
        "-Xprogressive".takeIf { hasProperty("test.progressive.mode") } // TODO: change to "-progressive" after bootstrap
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
        if (javaHome != defaultJavaHome || jvmTarget != defaultJvmTarget) {
            logger.info("configuring project $name to compile to the target jvm version $jvmTarget using jdk: $javaHome")
            configureJvmProject(javaHome!!, jvmTarget!!)
        } // else we will actually fail during the first task execution. We could not fail before configuration is done due to impact on import in IDE

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
    create("clean") {
        doLast {
            delete("$buildDir/repo")
            delete(distDir)
        }
    }

    create("cleanupArtifacts") {
        doLast {
            delete(ideaPluginDir)
            delete(ideaUltimatePluginDir)
            delete(cidrPluginDir)
            delete(appcodePluginDir)
            delete(clionPluginDir)
        }
    }

    listOf("clean", "assemble", "install", "dist").forEach { taskName ->
        create("coreLibs${taskName.capitalize()}") {
            coreLibProjects.forEach { projectName -> dependsOn("$projectName:$taskName") }
        }
    }

    create("coreLibsTest") {
        (coreLibProjects + listOf(
                ":kotlin-stdlib-jre7",
                ":kotlin-stdlib-jre8",
                ":kotlin-stdlib:samples",
                ":kotlin-test:kotlin-test-js:kotlin-test-js-it",
                ":kotlinx-metadata-jvm",
                ":tools:binary-compatibility-validator"
        )).forEach {
            dependsOn(it + ":check")
        }
    }

    create("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn(it + ":check")
        }
    }

    create("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    create("jvmCompilerTest") {
        dependsOn("dist")
        dependsOn(":compiler:test",
                  ":compiler:container:test",
                  ":compiler:tests-java8:test",
                  ":compiler:tests-spec:remoteRunTests")
        dependsOn(":plugins:jvm-abi-gen:test")
    }

    create("jsCompilerTest") {
        dependsOn(":js:js.tests:test")
        dependsOn(":js:js.tests:runMocha")
    }

    create("scriptingTest") {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
        dependsOn(":kotlin-scripting-jvm-host:test")
    }

    create("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")

        dependsOn("scriptingTest")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":core:descriptors.runtime:test")
    }

    create("toolsTest") {
        dependsOn(":tools:kotlinp:test")
    }

    create("examplesTest") {
        dependsOn("dist")
        (project(":examples").subprojects + project(":kotlin-gradle-subplugin-example")).forEach { p ->
            dependsOn("${p.path}:check")
        }
    }

    create("distTest") {
        dependsOn("compilerTest")
        dependsOn("toolsTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }

    create("specTest") {
        dependsOn("dist")
        dependsOn(":compiler:tests-spec:test")
    }

    create("androidCodegenTest") {
        dependsOn(":compiler:android-tests:test")
    }

    create("jps-tests") {
        dependsOn("dist")
        dependsOn(":jps-plugin:test")
    }

    create("idea-plugin-main-tests") {
        dependsOn("dist")
        dependsOn(":idea:test")
    }

    create("idea-plugin-additional-tests") {
        dependsOn("dist")
        dependsOn(":idea:idea-gradle:test",
                  ":idea:idea-maven:test",
                  ":j2k:test",
                  ":eval4j:test")
    }

    create("idea-plugin-tests") {
        dependsOn("dist")
        dependsOn("idea-plugin-main-tests",
                  "idea-plugin-additional-tests")
    }

    create("android-ide-tests") {
        dependsOn("dist")
        dependsOn(":plugins:android-extensions-ide:test",
                  ":idea:idea-android:test",
                  ":kotlin-annotation-processing:test")
    }

    create("plugins-tests") {
        dependsOn("dist")
        dependsOn(":kotlin-annotation-processing:test",
                  ":kotlin-source-sections-compiler-plugin:test",
                  ":kotlin-allopen-compiler-plugin:test",
                  ":kotlin-noarg-compiler-plugin:test",
                  ":kotlin-sam-with-receiver-compiler-plugin:test",
                  ":plugins:uast-kotlin:test",
                  ":kotlin-annotation-processing-gradle:test",
                  ":kotlinx-serialization-ide-plugin:test")
    }


    create("ideaPluginTest") {
        dependsOn(
                "idea-plugin-tests",
                "jps-tests",
                "plugins-tests",
                "android-ide-tests",
                ":generators:test"
        )
    }


    create("test") {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }

    create("check") {
        dependsOn("test")
    }
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
    from("libraries/stdlib/common/test") { into("stdlib/common") }
    from("libraries/stdlib/test") { into("stdlib/test") }
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

fun cidrPlugin(product: String, pluginDir: String) = tasks.creating(Copy::class.java) {
    if (!includeCidr) {
        throw GradleException("CIDR plugins require 'cidrPluginsEnabled' property turned on")
    }
    val prepareCidrPlugin = getTasksByName("cidrPlugin", true)
    val prepareCurrentPlugin = (getTasksByName(product.toLowerCase() + "Plugin", true) - this)
    prepareCurrentPlugin.forEach { it.mustRunAfter(prepareCidrPlugin) }

    dependsOn(ideaPlugin)
    dependsOn(prepareCidrPlugin)
    dependsOn(prepareCurrentPlugin)

    into(pluginDir)
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
    from(configurations[product.toLowerCase() + "KotlinPlugin"]) { into("lib") }
}

fun zipCidrPlugin(product: String, productVersion: String) = tasks.creating(Zip::class.java) {
    // Note: "cidrPluginVersion" has different format and semantics from "pluginVersion" used in IJ and AS plugins.
    val cidrPluginVersion = project.findProperty("cidrPluginVersion") as String? ?: "beta-1"
    val destPath = project.findProperty("pluginZipPath") as String?
            ?: "$distDir/artifacts/kotlin-plugin-$kotlinVersion-$product-$cidrPluginVersion-$productVersion.zip"
    val destFile = File(destPath)

    destinationDir = destFile.parentFile
    archiveName = destFile.name

    from(tasks[product.toLowerCase() + "Plugin"])
    into("Kotlin")
    setExecutablePermissions()

    doLast {
        logger.lifecycle("Plugin artifacts packed to $archivePath")
    }
}

if (includeCidr) {
    val appcodePlugin by cidrPlugin("AppCode", appcodePluginDir)
    val appcodeVersion = extra["versions.appcode"] as String
    val zipAppCodePlugin by zipCidrPlugin("AppCode", appcodeVersion)

    val clionPlugin by cidrPlugin("CLion", clionPluginDir)
    val clionVersion = extra["versions.clion"] as String
    val zipCLionPlugin by zipCidrPlugin("CLion", clionVersion)
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

fun jdkPath(version: String): String {
    val jdkName = "JDK_${version.replace(".", "")}"
    val jdkMajorVersion = JdkMajorVersion.valueOf(jdkName)
    return configuredJdks.find { it.majorVersion == jdkMajorVersion }?.homeDir?.canonicalPath?:jdkNotFoundConst
}


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

allprojects {
    afterEvaluate {
        if (cacheRedirectorEnabled()) {
            logger.info("Redirecting repositories for $displayName")
            repositories.redirect()
        }
    }
}
