import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask
import org.gradle.kotlin.dsl.*

buildscript {
    extra["defaultSnapshotVersion"] = "1.3-SNAPSHOT"

    // when updating please also update JPS artifacts configuration: https://jetbrains.quip.com/zzGUAYSJ6gv3/JPS-Build-update-bootstrap
    kotlinBootstrapFrom(BootstrapOption.TeamCity("1.3.50-dev-526", onlySuccessBootstrap = false))

    repositories {
        bootstrapKotlinRepo?.let(::maven)

        val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        } else {
            maven("https://plugins.gradle.org/m2")
        }
    }

    // a workaround for kotlin compiler classpath in kotlin project: sometimes gradle substitutes
    // kotlin-stdlib external dependency with local project :kotlin-stdlib in kotlinCompilerClasspath configuration.
    // see also configureCompilerClasspath@
    val bootstrapCompilerClasspath by configurations.creating

    dependencies {
        bootstrapCompilerClasspath(kotlin("compiler-embeddable", bootstrapKotlinVersion))

        classpath("com.gradle.publish:plugin-publish-plugin:0.9.7")
        classpath(kotlin("gradle-plugin", bootstrapKotlinVersion))
        classpath("net.sf.proguard:proguard-gradle:6.1.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.17")
    }
}

plugins {
    `build-scan`
    idea
    id("jps-compatible")
    id("org.jetbrains.gradle.plugin.idea-ext")
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
        it.forEach { jdkId ->
            logger.info("Using ${jdkId.majorVersion} home: ${jdkId.homeDir}")
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

val jpsBootstrap by configurations.creating

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val ideaUltimateSandboxDir = "$commonLocalDataDir/ideaUltimateSandbox"
val artifactsDir = "$distDir/artifacts"
val ideaPluginDir = "$artifactsDir/ideaPlugin/Kotlin"
val ideaUltimatePluginDir = "$artifactsDir/ideaUltimatePlugin/Kotlin"

extra["ktorExcludesForDaemon"] = listOf(
    "org.jetbrains.kotlin" to "kotlin-reflect",
    "org.jetbrains.kotlin" to "kotlin-stdlib",
    "org.jetbrains.kotlin" to "kotlin-stdlib-common",
    "org.jetbrains.kotlin" to "kotlin-stdlib-jdk8",
    "org.jetbrains.kotlin" to "kotlin-stdlib-jdk7",
    "org.jetbrains.kotlinx" to "kotlinx-coroutines-jdk8",
    "org.jetbrains.kotlinx" to "kotlinx-coroutines-core",
    "org.jetbrains.kotlinx" to "kotlinx-coroutines-core-common"
)

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["libsDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaUltimateSandboxDir"] = project.file(ideaUltimateSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["ideaUltimatePluginDir"] = project.file(ideaUltimatePluginDir)
extra["isSonatypeRelease"] = false

// Work-around necessary to avoid setting null javaHome. Will be removed after support of lazy task configuration
val jdkNotFoundConst = "JDK NOT FOUND"

extra["JDK_16"] = jdkPath("1.6")
extra["JDK_17"] = jdkPath("1.7")
extra["JDK_18"] = jdkPath("1.8")
extra["JDK_9"] = jdkPath("9")
extra["JDK_10"] = jdkPath("10")
extra["JDK_11"] = jdkPath("11")

// allow opening the project without setting up all env variables (see KT-26413)
if (!kotlinBuildProperties.isInIdeaSync) {
    checkJDK()
}

fun checkJDK() {
    val missingEnvVars = JdkMajorVersion.values()
        .filter { it.isMandatory() && extra[it.name] == jdkNotFoundConst }
        .mapTo(ArrayList()) { it.name }

    if (missingEnvVars.isNotEmpty()) {
        throw GradleException("Required environment variables are missing: ${missingEnvVars.joinToString()}")
    }
}

rootProject.apply {
    from(rootProject.file("gradle/versions.gradle.kts"))
    from(rootProject.file("gradle/report.gradle.kts"))
    from(rootProject.file("gradle/javaInstrumentation.gradle.kts"))
    from(rootProject.file("gradle/jps.gradle.kts"))
}

IdeVersionConfigurator.setCurrentIde(this)

extra["versions.protobuf"] = "2.6.1"
extra["versions.javax.inject"] = "1"
extra["versions.jsr305"] = "1.3.9"
extra["versions.jansi"] = "1.16"
extra["versions.jline"] = "3.3.1"
extra["versions.junit"] = "4.12"
extra["versions.javaslang"] = "2.0.6"
extra["versions.ant"] = "1.8.2"
extra["versions.android"] = "2.3.1"
val coroutinesVersion = if (Platform[192].orHigher()) "1.2.1" else "1.1.1"
extra["versions.kotlinx-coroutines-core"] = coroutinesVersion
extra["versions.kotlinx-coroutines-jdk8"] = coroutinesVersion
extra["versions.json"] = "20160807"
extra["versions.native-platform"] = "0.14"
extra["versions.ant-launcher"] = "1.8.0"
extra["versions.robolectric"] = "3.1"
extra["versions.org.springframework"] = "4.2.0.RELEASE"
extra["versions.jflex"] = "1.7.0"
extra["versions.markdown"] = "0.1.25"
extra["versions.trove4j"] = "1.0.20181211"

// NOTE: please, also change KTOR_NAME in pathUtil.kt and all versions in corresponding jar names in daemon tests.
extra["versions.ktor-network"] = "1.0.1"

if (!project.hasProperty("versions.kotlin-native")) {
    extra["versions.kotlin-native"] = "1.3.50-dev-11052"
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild
val intellijUltimateEnabled by extra(project.kotlinBuildProperties.intellijUltimateEnabled)
val effectSystemEnabled by extra(project.getBooleanProperty("kotlin.compiler.effectSystemEnabled") ?: false)
val newInferenceEnabled by extra(project.getBooleanProperty("kotlin.compiler.newInferenceEnabled") ?: false)

val intellijSeparateSdks = project.getBooleanProperty("intellijSeparateSdks") ?: false

extra["intellijSeparateSdks"] = intellijSeparateSdks

extra["IntellijCoreDependencies"] =
    listOf(
        if (Platform[191].orHigher()) "asm-all-7.0.1" else "asm-all",
        "guava",
        "jdom",
        "jna",
        "log4j",
        "picocontainer",
        "snappy-in-java",
        "streamex",
        "trove4j"
    )


extra["compilerModules"] = arrayOf(
    ":compiler:util",
    ":compiler:container",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:psi",
    ":compiler:frontend",
    ":compiler:frontend.common",
    ":compiler:frontend.java",
    ":compiler:cli-common",
    ":compiler:ir.tree",
    ":compiler:ir.psi2ir",
    ":compiler:ir.backend.common",
    ":compiler:backend.jvm",
    ":compiler:backend.js",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.js",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":compiler:backend-common",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:light-classes",
    ":compiler:cli",
    ":compiler:cli-js",
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
    ":core:util.runtime",
    ":core:type-system",
    ":compiler:fir:cones",
    ":compiler:fir:resolve",
    ":compiler:fir:tree",
    ":compiler:fir:psi2fir",
    ":compiler:fir:fir2ir",
    ":compiler:fir:java"
)

val coreLibProjects = listOfNotNull(
    ":kotlin-stdlib",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib-js",
    // Local builds are disabled at the request of the lib team
    // TODO: Enable when tests are fixed
    ":kotlin-stdlib-js-ir".takeIf { isTeamcityBuild },
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

    configurations.maybeCreate("embedded")

    jvmTarget = defaultJvmTarget
    javaHome = defaultJavaHome

    // There are problems with common build dir:
    //  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
    //  - idea seems unable to exclude common buildDir from indexing
    // therefore it is disabled by default
    // buildDir = File(commonBuildDir, project.name)

    val mirrorRepo: String? = findProperty("maven.repository.mirror")?.toString()

    repositories {
        kotlinBuildLocalRepo(project)
        mirrorRepo?.let(::maven)
        jcenter()
        maven(protobufRepo)
        maven(intellijRepo)
        maven("https://dl.bintray.com/kotlin/ktor")
        maven("https://kotlin.bintray.com/kotlin-dependencies")
        bootstrapKotlinRepo?.let(::maven)
        internalKotlinRepo?.let(::maven)
    }

    configureJvmProject(javaHome!!, jvmTarget!!)

    val commonCompilerArgs = listOfNotNull(
        "-Xuse-experimental=kotlin.Experimental",
        "-Xallow-kotlin-package",
        "-Xread-deserialized-contracts",
        "-Xjvm-default=compatibility",
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
            println("${project.path} $role classpath:\n  ${joinToString("\n  ") { it.toProjectRootRelativePathOrSelf() }}")

        try {
            javaPluginConvention()
        } catch (_: UnknownDomainObjectException) {
            null
        }?.let { javaConvention ->
            task("printCompileClasspath") { doFirst { javaConvention.sourceSets["main"].compileClasspath.printClassPath("compile") } }
            task("printRuntimeClasspath") { doFirst { javaConvention.sourceSets["main"].runtimeClasspath.printClassPath("runtime") } }
            task("printTestCompileClasspath") { doFirst { javaConvention.sourceSets["test"].compileClasspath.printClassPath("test compile") } }
            task("printTestRuntimeClasspath") { doFirst { javaConvention.sourceSets["test"].runtimeClasspath.printClassPath("test runtime") } }
        }

        run configureCompilerClasspath@{
            val bootstrapCompilerClasspath by rootProject.buildscript.configurations
            configurations.findByName("kotlinCompilerClasspath")?.let {
                dependencies.add(it.name, files(bootstrapCompilerClasspath))
            }
        }

        // Aggregate task for build related checks
        tasks.register("checkBuild")
        
        apply(from = "$rootDir/gradle/cacheRedirector.gradle.kts")
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

val dist = tasks.register("dist") {
    dependsOn(":kotlin-compiler:dist")
}

val copyCompilerToIdeaPlugin by task<Copy> {
    dependsOn(dist)
    into(ideaPluginDir)
    from(distDir) { include("kotlinc/**") }
}

val ideaPlugin by task<Task> {
    dependsOn(copyCompilerToIdeaPlugin)
    dependsOn(":prepare:idea-plugin:ideaPlugin")
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
            delete(artifactsDir)
        }
    }

    listOf("clean", "assemble", "install", "dist").forEach { taskName ->
        create("coreLibs${taskName.capitalize()}") {
            coreLibProjects.forEach { projectName -> dependsOn("$projectName:$taskName") }
        }
    }

    create("coreLibsTest") {
        (coreLibProjects + listOf(
            ":kotlin-stdlib:samples",
            ":kotlin-test:kotlin-test-js:kotlin-test-js-it",
            ":kotlinx-metadata-jvm",
            ":tools:binary-compatibility-validator"
        )).forEach {
            dependsOn("$it:check")
        }
    }

    create("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn("$it:check")
        }
    }

    create("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    create("jvmCompilerTest") {
        dependsOn("dist")
        dependsOn(
            ":compiler:test",
            ":compiler:container:test",
            ":compiler:tests-java8:test",
            ":compiler:tests-spec:remoteRunTests"
        )
        dependsOn(":plugins:jvm-abi-gen:test")
    }

    create("jsCompilerTest") {
        dependsOn(":js:js.tests:test")
        dependsOn(":js:js.tests:runMocha")
    }

    create("firCompilerTest") {
        dependsOn(":compiler:fir:psi2fir:test")
        dependsOn(":compiler:fir:resolve:test")
        dependsOn(":compiler:fir:fir2ir:test")
        dependsOn(":compiler:fir:lightTree:test")
    }

    create("scriptingTest") {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
        dependsOn(":kotlin-scripting-compiler:test")
        dependsOn(":kotlin-scripting-jvm-host-test:test")
        dependsOn(":kotlin-scripting-jsr223-test:test")
        dependsOn(":kotlin-scripting-jvm-host-test:embeddableTest")
        dependsOn(":kotlin-scripting-jsr223-test:embeddableTest")
        dependsOn(":kotlin-main-kts-test:test")
    }

    create("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")
        dependsOn("firCompilerTest")

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
        dependsOn(
            ":idea:idea-gradle:test",
            ":idea:idea-gradle-native:test",
            ":idea:idea-maven:test",
            ":j2k:test",
            ":nj2k:test",
            ":idea:jvm-debugger:jvm-debugger-core:test",
            ":idea:jvm-debugger:jvm-debugger-evaluation:test",
            ":idea:jvm-debugger:jvm-debugger-sequence:test",
            ":idea:jvm-debugger:eval4j:test"
        )
    }

    create("idea-plugin-tests") {
        dependsOn("dist")
        dependsOn(
            "idea-plugin-main-tests",
            "idea-plugin-additional-tests"
        )
    }

    create("android-ide-tests") {
        dependsOn("dist")
        dependsOn(
            ":plugins:android-extensions-ide:test",
            ":idea:idea-android:test",
            ":kotlin-annotation-processing:test"
        )
    }

    create("plugins-tests") {
        dependsOn("dist")
        dependsOn(
            ":kotlin-annotation-processing:test",
            ":kotlin-source-sections-compiler-plugin:test",
            ":kotlin-allopen-compiler-plugin:test",
            ":kotlin-noarg-compiler-plugin:test",
            ":kotlin-sam-with-receiver-compiler-plugin:test",
            ":plugins:uast-kotlin:test",
            ":kotlin-annotation-processing-gradle:test",
            ":kotlinx-serialization-compiler-plugin:test",
            ":kotlinx-serialization-ide-plugin:test"
        )
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

val zipStdlibTests by task<Zip> {
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-stdlib-tests.zip")
    from("libraries/stdlib/common/test") { into("common") }
    from("libraries/stdlib/test") { into("test") }
    doLast {
        logger.lifecycle("Stdlib tests are packed to ${archiveFile.get()}")
    }
}

val zipTestData by task<Zip> {
    dependsOn(zipStdlibTests)
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-test-data.zip")
    isZip64 = true
    from("compiler/testData") { into("compiler") }
    from("idea/testData") { into("ide") }
    from("idea/idea-completion/testData") { into("ide/completion") }
    from("compiler/tests-common/tests/org/jetbrains/kotlin/coroutineTestUtil.kt") { into("compiler") }
    doLast {
        logger.lifecycle("Test data packed to ${archiveFile.get()}")
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
    return configuredJdks.find { it.majorVersion == jdkMajorVersion }?.homeDir?.canonicalPath ?: jdkNotFoundConst
}


fun Project.configureJvmProject(javaHome: String, javaVersion: String) {
    val currentJavaHome = File(System.getProperty("java.home")!!).canonicalPath
    val shouldFork = !currentJavaHome.startsWith(File(javaHome).canonicalPath)

    tasks.withType<JavaCompile> {
        if (name != "compileJava9Java") {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
            options.isFork = shouldFork
            options.forkOptions.javaHome = file(javaHome)
            options.compilerArgs.add("-proc:none")
            options.encoding = "UTF-8"
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jdkHome = javaHome
        kotlinOptions.jvmTarget = javaVersion
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
    }

    tasks.withType<Test> {
        executable = File(javaHome, "bin/java").canonicalPath
    }

    plugins.withId("java-base") {
        configureShadowJarSubstitutionInCompileClasspath()
    }
}

fun Project.configureShadowJarSubstitutionInCompileClasspath() {
    val substitutionMap = mapOf(":kotlin-reflect" to ":kotlin-reflect-api")

    fun configureSubstitution(substitution: DependencySubstitution) {
        val requestedProject = (substitution.requested as? ProjectComponentSelector)?.projectPath ?: return
        val replacementProject = substitutionMap[requestedProject] ?: return
        substitution.useTarget(project(replacementProject), "Non-default shadow jars should not be used in compile classpath")
    }

    sourceSets.all {
        for (configName in listOf(compileOnlyConfigurationName, compileClasspathConfigurationName)) {
            configurations.getByName(configName).resolutionStrategy.dependencySubstitution {
                all(::configureSubstitution)
            }
        }
    }
}

tasks.create("findShadowJarsInClasspath").doLast {
    fun Collection<File>.printSorted(indent: String = "    ") {
        sortedBy { it.path }.forEach { println(indent + it.relativeTo(rootProject.projectDir)) }
    }

    val mainJars = hashSetOf<File>()
    val shadowJars = hashSetOf<File>()
    for (project in rootProject.allprojects) {
        project.withJavaPlugin {
            project.sourceSets.forEach { sourceSet ->
                val jarTask = project.tasks.findByPath(sourceSet.jarTaskName) as? Jar
                jarTask?.outputFile?.let { mainJars.add(it) }
            }
        }
        for (task in project.tasks) {
            when (task) {
                is ShadowJar -> {
                    shadowJars.add(fileFrom(task.outputFile))
                }
                is ProGuardTask -> {
                    shadowJars.addAll(task.outputs.files.toList())
                }
            }
        }
    }

    shadowJars.removeAll(mainJars)
    println("Shadow jars that might break incremental compilation:")
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
        project.sourceSetsOrNull?.forEach { sourceSet ->
            project.checkConfig(sourceSet.compileClasspathConfigurationName)
        }
    }
}

val Jar.outputFile: File
    get() = archiveFile.get().asFile

val Project.sourceSetsOrNull: SourceSetContainer?
    get() = convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets
