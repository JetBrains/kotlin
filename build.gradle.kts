import org.gradle.crypto.checksum.Checksum
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport

buildscript {
    // a workaround for kotlin compiler classpath in kotlin project: sometimes gradle substitutes
    // kotlin-stdlib external dependency with local project :kotlin-stdlib in kotlinCompilerClasspath configuration.
    // see also configureCompilerClasspath@
    val bootstrapCompilerClasspath by configurations.creating

    dependencies {
        bootstrapCompilerClasspath(kotlin("compiler-embeddable", bootstrapKotlinVersion))

        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    }

    val versionPropertiesFile = project.rootProject.projectDir.resolve("gradle/versions.properties")
    val versionProperties = java.util.Properties()
    versionPropertiesFile.inputStream().use { propInput ->
        versionProperties.load(propInput)
    }
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion(versionProperties["versions.gson"] as String)
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }
}

plugins {
    base
    idea
    id("jps-compatible")
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("org.gradle.crypto.checksum") version "1.2.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.12.0" apply false
    signing
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
}

pill {
    excludedDirs(
        "out",
        "buildSrc/build",
        "buildSrc/prepare-deps/intellij-sdk/build",
        "intellij"
    )
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

val defaultSnapshotVersion: String by extra
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(
    findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
        if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else defaultSnapshotVersion
    } ?: buildNumber
)

val kotlinLanguageVersion by extra("1.8")

extra["kotlin_root"] = rootDir

val jpsBootstrap by configurations.creating

val commonBuildDir = File(rootDir, "build")
val distDir by extra("$rootDir/dist")
val distKotlinHomeDir by extra("$distDir/kotlinc")
val distLibDir = "$distKotlinHomeDir/lib"
val commonLocalDataDir = "$rootDir/local"
val ideaSandboxDir = "$commonLocalDataDir/ideaSandbox"
val artifactsDir = "$distDir/artifacts"
val ideaPluginDir = "$artifactsDir/ideaPlugin/Kotlin"

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
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)
extra["isSonatypeRelease"] = false

rootProject.apply {
    from(rootProject.file("gradle/versions.gradle.kts"))
    from(rootProject.file("gradle/report.gradle.kts"))
    from(rootProject.file("gradle/javaInstrumentation.gradle.kts"))
    from(rootProject.file("gradle/jps.gradle.kts"))
    from(rootProject.file("gradle/checkArtifacts.gradle.kts"))
    from(rootProject.file("gradle/checkCacheability.gradle.kts"))
    from(rootProject.file("gradle/retryPublishing.gradle.kts"))
    from(rootProject.file("gradle/modularizedTestConfigurations.gradle.kts"))
    from(rootProject.file("gradle/ideaRtHack.gradle.kts"))
}

IdeVersionConfigurator.setCurrentIde(project)

if (!project.hasProperty("versions.kotlin-native")) {
    // BEWARE! Bumping this version doesn't take an immediate effect on TeamCity: KTI-1107
    extra["versions.kotlin-native"] = "1.9.0-dev-920"
}

val irCompilerModules = arrayOf(
    ":compiler:ir.tree",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.js",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.backend.common",
    ":compiler:ir.interpreter",
    ":wasm:wasm.ir"
).also { extra["irCompilerModules"] = it }

val commonCompilerModules = arrayOf(
    ":compiler:psi",
    ":compiler:frontend.common-psi",
    ":analysis:light-classes-base",
    ":compiler:frontend.common",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":compiler:cli-common",
    ":compiler:resolution.common",
    ":compiler:resolution.common.jvm",
    ":core:metadata",
    ":core:metadata.jvm",
    ":core:deserialization.common",
    ":core:deserialization.common.jvm",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:util.runtime",
    ":compiler:frontend.common.jvm",
    ":compiler:frontend.java", // TODO this is fe10 module but some utils used in fir ide now
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":analysis:kt-references",
).also { extra["commonCompilerModules"] = it }

val firCompilerCoreModules = arrayOf(
    ":compiler:fir:cones",
    ":compiler:fir:providers",
    ":compiler:fir:semantics",
    ":compiler:fir:resolve",
    ":compiler:fir:fir-serialization",
    ":compiler:fir:fir-deserialization",
    ":compiler:fir:plugin-utils",
    ":compiler:fir:tree",
    ":compiler:fir:java",
    ":compiler:fir:raw-fir:raw-fir.common",
    ":compiler:fir:raw-fir:psi2fir",
    ":compiler:fir:checkers",
    ":compiler:fir:checkers:checkers.jvm",
    ":compiler:fir:checkers:checkers.js",
    ":compiler:fir:checkers:checkers.native",
    ":compiler:fir:entrypoint", // TODO should not be in core modules but FIR IDE uses DependencyListForCliModule from this module
    ":compiler:fir:fir2ir:jvm-backend",  // TODO should not be in core modules but FIR IDE uses Fir2IrSignatureComposer from this module
    ":compiler:fir:fir2ir" // TODO should not be in core modules but FIR IDE uses Fir2IrSignatureComposer from this module
).also { extra["firCompilerCoreModules"] = it }

val firAllCompilerModules = firCompilerCoreModules +
    arrayOf(
        ":compiler:fir:raw-fir:light-tree2fir",
        ":compiler:fir:analysis-tests",
        ":compiler:fir:analysis-tests:legacy-fir-tests"
    )

val fe10CompilerModules = arrayOf(
    ":compiler",
    ":core:descriptors.runtime",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":compiler:light-classes",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:frontend",
    ":compiler:container",
    ":compiler:cli-common",
    ":core:deserialization",
    ":compiler:frontend:cfg",
    ":compiler:ir.psi2ir",
    ":compiler:backend.jvm",
    ":compiler:backend.jvm.lower",
    ":compiler:backend.jvm.codegen",
    ":compiler:backend.jvm.entrypoint",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":compiler:backend-common",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:javac-wrapper",
    ":compiler:cli",
    ":compiler:cli-js",
    ":compiler:incremental-compilation-impl",
    ":compiler:compiler.version",
    ":js:js.ast",
    ":js:js.sourcemap",
    ":js:js.serializer",
    ":js:js.parser",
    ":compiler:config.web",
    ":js:js.config",
    ":js:js.frontend",
    ":js:js.translator",
    ":js:js.dce",
    ":native:frontend.native",
    ":native:kotlin-native-utils",
    ":kotlin-build-common",
    ":compiler:backend.common.jvm",
    ":analysis:decompiled:light-classes-for-decompiled-fe10",
).also { extra["fe10CompilerModules"] = it }

extra["compilerModules"] =
    irCompilerModules +
            fe10CompilerModules +
            commonCompilerModules +
            firAllCompilerModules

// They are embedded just because we don't publish those dependencies as separate Maven artifacts (yet)
extra["kotlinJpsPluginEmbeddedDependencies"] = listOf(
    ":compiler:cli-common",
    ":kotlin-compiler-runner-unshaded",
    ":daemon-common",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":compiler:backend.common.jvm",
    ":js:js.serializer",
    ":core:deserialization",
    ":core:deserialization.common",
    ":core:deserialization.common.jvm",
    ":compiler:frontend.common.jvm",
    ":compiler:frontend.java",
    ":core:metadata",
    ":core:metadata.jvm",
    ":kotlin-preloader",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":compiler:config.web",
    ":js:js.config",
    ":core:util.runtime",
    ":compiler:compiler.version"
)

extra["kotlinJpsPluginMavenDependencies"] = listOf(
    ":kotlin-daemon-client",
    ":kotlin-build-common",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":native:kotlin-native-utils"
)

extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"] = listOf(
    commonDependency("org.jetbrains.kotlin:kotlin-reflect")
)

extra["compilerArtifactsForIde"] = listOfNotNull(
    ":prepare:ide-plugin-dependencies:android-extensions-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide",
    ":prepare:ide-plugin-dependencies:js-ir-runtime-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-cli-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-gradle-statistics-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-classpath",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-testdata-for-ide",
    ":prepare:ide-plugin-dependencies:kotlinx-serialization-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:noarg-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:sam-with-receiver-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:assignment-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:parcelize-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:lombok-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-backend-native-for-ide".takeIf { kotlinBuildProperties.isKotlinNativeEnabled },
    ":prepare:ide-plugin-dependencies:kotlin-compiler-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-testdata-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-stdlib-minimal-for-test-for-ide",
    ":prepare:ide-plugin-dependencies:low-level-api-fir-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-impl-base-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-impl-base-tests-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-fir-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-fir-tests-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:high-level-api-fe10-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kt-references-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-providers-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-project-structure-for-ide",
    ":prepare:ide-plugin-dependencies:symbol-light-classes-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-standalone-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-ir-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fir-for-ide",
    ":prepare:kotlin-jps-plugin",
    ":kotlin-script-runtime",
    ":kotlin-script-util",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-android-extensions-runtime",
    ":plugins:parcelize:parcelize-runtime",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-stdlib-js",
    ":kotlin-test",
    ":kotlin-daemon",
    ":kotlin-compiler",
    ":kotlin-annotations-jvm",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-reflect",
    ":kotlin-main-kts"
)

val coreLibProjects by extra {
    listOfNotNull(
        ":kotlin-stdlib",
        ":kotlin-stdlib-common",
        ":kotlin-stdlib-js",
        ":kotlin-stdlib-jdk7",
        ":kotlin-stdlib-jdk8",
        ":kotlin-test",
        ":kotlin-test:kotlin-test-annotations-common",
        ":kotlin-test:kotlin-test-common",
        ":kotlin-test:kotlin-test-jvm",
        ":kotlin-test:kotlin-test-junit",
        ":kotlin-test:kotlin-test-junit5",
        ":kotlin-test:kotlin-test-testng",
        ":kotlin-test:kotlin-test-js".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
        ":kotlin-reflect"
    )
}

val projectsWithEnabledContextReceivers by extra {
    listOf(
        ":core:descriptors.jvm",
        ":compiler:frontend.common",
        ":compiler:fir:resolve",
        ":compiler:fir:plugin-utils",
        ":compiler:fir:fir2ir",
        ":kotlin-lombok-compiler-plugin.k1",
        ":kotlinx-serialization-compiler-plugin.k2",
        ":plugins:parcelize:parcelize-compiler:parcelize.k2",
        ":plugins:fir-plugin-prototype"
    )
}

val projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib by extra {
    listOf(
        ":analysis:analysis-api-fe10",
        ":analysis:analysis-api-fir",
        ":analysis:decompiled:light-classes-for-decompiled",
        ":analysis:symbol-light-classes",
        ":compiler",
        ":compiler:backend.js",
        ":compiler:light-classes",
        ":jps:jps-common",
        ":js:js.tests",
        ":kotlin-build-common",
        ":kotlin-gradle-plugin",
        ":kotlin-scripting-jvm-host-test",
        ":native:kotlin-klib-commonizer",
    )
}

val gradlePluginProjects = listOf(
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin-api",
    ":kotlin-gradle-plugin-annotations",
    ":kotlin-gradle-plugin-idea",
    ":kotlin-gradle-plugin-idea-proto",
    ":kotlin-gradle-plugin-kpm-android",
    ":kotlin-gradle-plugin-tcs-android",
    ":kotlin-allopen",
    ":kotlin-annotation-processing-gradle",
    ":kotlin-noarg",
    ":kotlin-sam-with-receiver",
    ":kotlin-parcelize-compiler",
    ":kotlin-lombok",
    ":kotlin-assignment"
)

val ignoreTestFailures by extra(project.kotlinBuildProperties.ignoreTestFailures)

val dependencyOnSnapshotReflectWhitelist = setOf(
    ":kotlin-compiler",
    ":kotlin-reflect",
    ":tools:binary-compatibility-validator",
    ":tools:kotlin-stdlib-gen",
)

allprojects {
    if (!project.path.startsWith(":kotlin-ide.")) {
        pluginManager.apply("common-configuration")
    }
    configurations.all {
        val configuration = this
        if (name != "compileClasspath") {
            return@all
        }
        resolutionStrategy.eachDependency {
            if (requested.group != "org.jetbrains.kotlin") {
                return@eachDependency
            }
            val isReflect = requested.name == "kotlin-reflect" || requested.name == "kotlin-reflect-api"
            // More strict check for "compilerModules". We can't apply this check for all modules because it would force to
            // exclude kotlin-reflect from transitive dependencies of kotlin-poet, ktor, com.android.tools.build:gradle, etc
            if (project.path in (rootProject.extra["compilerModules"] as Array<String>)) {
                val expectedReflectVersion = commonDependencyVersion("org.jetbrains.kotlin", "kotlin-reflect")
                if (isReflect) {
                    check(requested.version == expectedReflectVersion) {
                        """
                            $configuration: 'kotlin-reflect' should have '$expectedReflectVersion' version. But it was '${requested.version}'
                            Suggestions:
                                1. Use 'commonDependency("org.jetbrains.kotlin:kotlin-reflect") { isTransitive = false }'
                                2. Avoid 'kotlin-reflect' leakage from transitive dependencies with 'exclude("org.jetbrains.kotlin")'
                        """.trimIndent()
                    }
                }
                if (requested.name.startsWith("kotlin-stdlib")) {
                    check(requested.version != expectedReflectVersion) {
                        """
                            $configuration: '${requested.name}' has a wrong version. It's not allowed to be '$expectedReflectVersion'
                            Suggestions:
                                1. Most likely, it leaked from 'kotlin-reflect' transitive dependencies. Use 'isTransitive = false' for
                                   'kotlin-reflect' dependencies
                                2. Avoid '${requested.name}' leakage from other transitive dependencies with 'exclude("org.jetbrains.kotlin")'
                        """.trimIndent()
                    }
                }
            }
            if (isReflect && project.path !in dependencyOnSnapshotReflectWhitelist) {
                check(requested.version != kotlinVersion) {
                    """
                        $configuration: 'kotlin-reflect' is not allowed to have '$kotlinVersion' version.
                        Suggestion: Use 'commonDependency("org.jetbrains.kotlin:kotlin-reflect") { isTransitive = false }'
                    """.trimIndent()
                }
            }
        }
    }
    val mirrorRepo: String? = findProperty("maven.repository.mirror")?.toString()

    repositories {
        when (kotlinBuildProperties.getOrNull("attachedIntellijVersion")) {
            null -> {}
            "master" -> {
                maven { setUrl("https://www.jetbrains.com/intellij-repository/snapshots") }
            }

            else -> {
                kotlinBuildLocalRepo(project)
            }
        }

        mirrorRepo?.let(::maven)

        maven(intellijRepo) {
            content {
                includeGroupByRegex("com\\.jetbrains\\.intellij(\\..+)?")
            }
        }

        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
            content {
                includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
                includeVersion("org.jetbrains.jps", "jps-javac-extension", "1")
            }
        }

        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") {
            content {
                includeModule("org.jetbrains.dukat", "dukat")
                includeModule("org.jetbrains.kotlin", "android-dx")
                includeModule("org.jetbrains.kotlin", "jcabi-aether")
                includeModule("org.jetbrains.kotlin", "kotlin-build-gradle-plugin")
                includeModule("org.jetbrains.kotlin", "protobuf-lite")
                includeModule("org.jetbrains.kotlin", "protobuf-relocated")
                includeModule("org.jetbrains.kotlinx", "kotlinx-metadata-klib")
                includeGroup("org.jetbrains.dokka")
            }
        }

        maven("https://download.jetbrains.com/teamcity-repository") {
            content {
                includeModule("org.jetbrains.teamcity", "serviceMessages")
                includeModule("org.jetbrains.teamcity.idea", "annotations")
            }
        }

        maven("https://dl.google.com/dl/android/maven2") {
            content {
                includeGroup("com.android.tools")
                includeGroup("com.android.tools.build")
                includeGroup("com.android.tools.layoutlib")
                includeGroup("com.android")
                includeGroup("androidx.test")
                includeGroup("androidx.annotation")
            }
        }

        mavenCentral()

        @Suppress("DEPRECATION")
        jcenter {
            content {
                includeVersionByRegex("net\\.rubygrapefruit", ".+", "0\\.14")
                includeVersionByRegex("io\\.ktor", ".+", "1\\.1\\.5")
                includeVersion("khttp", "khttp", "1.0.0")
                includeVersion("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.4.2")
                includeVersion("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.14.0")
            }
        }
    }
}

apply {
    from("libraries/commonConfiguration.gradle")
}

if (extra.has("isDeployStagingRepoGenerationRequired") &&
    project.extra["isDeployStagingRepoGenerationRequired"] as Boolean == true
) {
    logger.info("Applying configuration for sonatype release")
    project.apply { from("libraries/prepareSonatypeStaging.gradle") }
}

gradle.taskGraph.whenReady {
    fun Boolean.toOnOff(): String = if (this) "on" else "off"
    val profile = if (isTeamcityBuild) "CI" else "Local"

    val proguardMessage = "proguard is ${kotlinBuildProperties.proguard.toOnOff()}"
    val jarCompressionMessage = "jar compression is ${kotlinBuildProperties.jarCompression.toOnOff()}"

    logger.warn(
        "$profile build profile is active ($proguardMessage, $jarCompressionMessage). " +
                "Use -Pteamcity=<true|false> to reproduce CI/local build"
    )

    allTasks.filterIsInstance<org.gradle.jvm.tasks.Jar>().forEach { task ->
        task.entryCompression = if (kotlinBuildProperties.jarCompression)
            ZipEntryCompression.DEFLATED
        else
            ZipEntryCompression.STORED
    }
}

val dist = tasks.register("dist") {
    dependsOn(":kotlin-compiler:dist")
}

val syncMutedTests = tasks.register("syncMutedTests") {
    dependsOn(":compiler:tests-mutes:tc-integration:run")
}

tasks.register("createIdeaHomeForTests") {
    val ideaBuildNumberFileForTests = ideaBuildNumberFileForTests()
    val intellijSdkVersion = rootProject.extra["versions.intellijSdk"]
    outputs.file(ideaBuildNumberFileForTests)
    doFirst {
        ideaBuildNumberFileForTests.parentFile.mkdirs()
        ideaBuildNumberFileForTests.writeText("IC-$intellijSdkVersion")
    }
}

tasks {
    named<Delete>("clean") {
        delete += setOf("$buildDir/repo", distDir)
    }

    register<Delete>("cleanupArtifacts") {
        delete = setOf(artifactsDir)
    }

    listOf("clean", "assemble", "install").forEach { taskName ->
        register("coreLibs${taskName.capitalize()}") {
            for (projectName in coreLibProjects) {
                if (projectName.startsWith(":kotlin-test:") && taskName == "install") continue
                dependsOn("$projectName:$taskName")
            }
        }
    }

    register("coreLibsTest") {
        (coreLibProjects + listOf(
            ":kotlin-stdlib:samples",
            ":kotlin-stdlib-js-ir",
            ":kotlin-test:kotlin-test-js-ir".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
            ":kotlin-test:kotlin-test-js:kotlin-test-js-it".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
            ":kotlin-test:kotlin-test-js-ir:kotlin-test-js-ir-it".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
            ":kotlinx-metadata-jvm",
            ":tools:binary-compatibility-validator",
            //":kotlin-stdlib-wasm",
        )).forEach {
            dependsOn("$it:check")
        }
    }

    register("gradlePluginTest") {
        gradlePluginProjects.forEach {
            dependsOn("$it:check")
        }
    }

    register("gradlePluginIntegrationTest") {
        dependsOn(":kotlin-gradle-plugin-integration-tests:check")
    }

    register("jvmCompilerTest") {
        dependsOn("dist")
        dependsOn(
            ":compiler:test",
            ":compiler:tests-common-new:test",
            ":compiler:container:test",
            ":compiler:tests-java8:test",
            ":compiler:tests-spec:test",
            ":compiler:tests-against-klib:test"
        )
    }

    register("testsForBootstrapBuildTest") {
        dependsOn("dist")
        dependsOn(":compiler:tests-common-new:test")
    }

    register("jvmCompilerIntegrationTest") {
        dependsOn(
            ":kotlin-compiler-embeddable:test",
            ":kotlin-compiler-client-embeddable:test"
        )
    }

    register("jsCompilerTest") {
        dependsOn(":js:js.tests:jsTest")
        dependsOn(":js:js.tests:runMocha")
    }

    register("jsFirCompilerTest") {
        dependsOn(":js:js.tests:jsFirTest")
    }

    register("jsIrCompilerTest") {
        dependsOn(":js:js.tests:jsIrTest")
        dependsOn(":js:js.tests:jsStdlibApiTest")
    }

    register("wasmCompilerTest") {
        dependsOn(":wasm:wasm.tests:test")
        // Windows WABT release requires Visual C++ Redistributable
        if (!kotlinBuildProperties.isTeamcityBuild || !org.gradle.internal.os.OperatingSystem.current().isWindows) {
            dependsOn(":wasm:wasm.ir:test")
        }
    }

    register("nativeCompilerTest") {
        dependsOn(":native:kotlin-native-utils:test")
    }

    register("firCompilerTest") {
        dependsOn(":compiler:fir:raw-fir:psi2fir:test")
        dependsOn(":compiler:fir:raw-fir:light-tree2fir:test")
        dependsOn(":compiler:fir:analysis-tests:test")
        dependsOn(":compiler:fir:analysis-tests:legacy-fir-tests:test")
        dependsOn(":compiler:fir:fir2ir:test")
    }

    register("firAllTest") {
        dependsOn(
            ":dist",
            ":compiler:fir:raw-fir:psi2fir:test",
            ":compiler:fir:raw-fir:light-tree2fir:test",
            ":compiler:fir:analysis-tests:test",
            ":compiler:fir:analysis-tests:legacy-fir-tests:test",
            ":compiler:fir:fir2ir:test",
        )
    }

    register("compilerFrontendVisualizerTest") {
        dependsOn("compiler:visualizer:test")
    }

    register("scriptingJvmTest") {
        dependsOn("dist")
        dependsOn(":kotlin-script-util:test")
        dependsOn(":kotlin-scripting-compiler:test")
        dependsOn(":kotlin-scripting-compiler:testWithIr")
        dependsOn(":kotlin-scripting-common:test")
        dependsOn(":kotlin-scripting-jvm:test")
        dependsOn(":kotlin-scripting-jvm-host-test:test")
        dependsOn(":kotlin-scripting-jvm-host-test:testWithIr")
        dependsOn(":kotlin-scripting-dependencies:test")
        dependsOn(":kotlin-scripting-dependencies-maven:test")
        dependsOn(":kotlin-scripting-jsr223-test:test")
        // see comments on the task in kotlin-scripting-jvm-host-test
//        dependsOn(":kotlin-scripting-jvm-host-test:embeddableTest")
        dependsOn(":kotlin-scripting-jsr223-test:embeddableTest")
        dependsOn(":kotlin-main-kts-test:test")
        dependsOn(":kotlin-scripting-ide-services-test:test")
        dependsOn(":kotlin-scripting-ide-services-test:embeddableTest")
    }

    register("scriptingK2Test") {
        dependsOn(":kotlin-scripting-compiler:testWithK2")
        dependsOn(":kotlin-scripting-jvm-host-test:testWithK2")
        dependsOn(":kotlin-main-kts-test:testWithK2")
    }

    register("scriptingTest") {
        dependsOn("scriptingJvmTest")
    }

    register("compilerTest") {
        dependsOn("jvmCompilerTest")
        dependsOn("jsCompilerTest")
        dependsOn("miscCompilerTest")
    }

    register("miscCompilerTest") {
        dependsOn("coreLibsTest")
        dependsOn("gradlePluginTest")
        dependsOn("toolsTest")
        dependsOn("examplesTest")
        dependsOn("nativeCompilerTest")
        dependsOn("incrementalCompilationTest")
        dependsOn("scriptingTest")
        dependsOn("jvmCompilerIntegrationTest")
        dependsOn("compilerPluginTest")

        dependsOn(":kotlin-daemon-tests:test")
        dependsOn(":kotlin-build-common:test")
        dependsOn(":kotlin-build-common:testJUnit5")
        dependsOn(":core:descriptors.runtime:test")
        dependsOn(":kotlin-util-io:test")
        dependsOn(":kotlin-util-klib:test")
        dependsOn(":generators:test")
    }

    register("incrementalCompilationTest") {
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":compiler:incremental-compilation-impl:testJvmICWithJdk11")
    }

    register("compilerPluginTest") {
        dependsOn(":kotlin-allopen-compiler-plugin:test")
        dependsOn(":kotlin-assignment-compiler-plugin:test")
        dependsOn(":kotlinx-atomicfu-compiler-plugin:test")
        dependsOn(":plugins:fir-plugin-prototype:test")
        dependsOn(":plugins:fir-plugin-prototype:fir-plugin-ic-test:test")
        dependsOn(":kotlin-imports-dumper-compiler-plugin:test")
        dependsOn(":plugins:jvm-abi-gen:test")
        dependsOn(":kotlinx-serialization-compiler-plugin:test")
        dependsOn(":kotlin-lombok-compiler-plugin:test")
        dependsOn(":kotlin-noarg-compiler-plugin:test")
        dependsOn(":kotlin-sam-with-receiver-compiler-plugin:test")
    }

    register("toolsTest") {
        dependsOn(":tools:kotlinp:test")
        dependsOn(":native:kotlin-klib-commonizer:test")
        dependsOn(":native:kotlin-klib-commonizer-api:test")
        dependsOn(":kotlin-tooling-core:check")
        dependsOn(":kotlin-tooling-metadata:check")
    }

    register("examplesTest") {
        dependsOn("dist")
        project(":examples").subprojects.forEach { p ->
            dependsOn("${p.path}:check")
        }
    }

    register("distTest") {
        dependsOn("compilerTest")
        dependsOn("frontendApiTests")
        dependsOn("toolsTest")
        dependsOn("gradlePluginTest")
        dependsOn("examplesTest")
    }

    register("specTest") {
        dependsOn("dist")
        dependsOn(":compiler:tests-spec:test")
    }

    register("androidCodegenTest") {
        dependsOn(":compiler:android-tests:test")
    }

    register("jps-tests") {
        dependsOn("dist")
        dependsOn(":jps:jps-plugin:test")
    }

    register("frontendApiTests") {
        dependsOn(":analysis:analysisAllTests")
    }

    register("kaptTests") {
        dependsOn(":kotlin-annotation-processing:test")
        dependsOn(":kotlin-annotation-processing:testJdk11")
        dependsOn(":kotlin-annotation-processing-base:test")
        dependsOn(":kotlin-annotation-processing-cli:test")
    }

    register("parcelizeTests") {
        dependsOn(":plugins:parcelize:parcelize-compiler:test")
    }

    register("codebaseTests") {
        dependsOn(":repo:codebase-tests:test")
    }

    register("test") {
        doLast {
            throw GradleException("Don't use directly, use aggregate tasks *-check instead")
        }
    }

    named("check") {
        dependsOn("test")
    }

    named("checkBuild") {
        if (kotlinBuildProperties.isTeamcityBuild) {
            doFirst {
                println("##teamcity[setParameter name='bootstrap.kotlin.version' value='$bootstrapKotlinVersion']")
            }
        }
    }

    register("publishGradlePluginArtifacts") {
        idePluginDependency {
            dependsOnKotlinGradlePluginPublish()
        }
    }

    register("publishIdeArtifacts") {
        idePluginDependency {
            @Suppress("UNCHECKED_CAST")
            dependsOn((rootProject.extra["compilerArtifactsForIde"] as List<String>).map { "$it:publish" })
        }
    }

    register("installIdeArtifacts") {
        idePluginDependency {
            @Suppress("UNCHECKED_CAST")
            dependsOn((rootProject.extra["compilerArtifactsForIde"] as List<String>).map { "$it:install" })
        }
    }
}

val zipCompiler by task<Zip> {
    dependsOn(dist)
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-compiler-$kotlinVersion.zip")

    from(distKotlinHomeDir)
    into("kotlinc")

    doLast {
        logger.lifecycle("Compiler artifacts packed to ${archiveFile.get().asFile.absolutePath}")
    }
}

val zipStdlibTests by task<Zip> {
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-stdlib-tests.zip")
    from("libraries/stdlib/common/test") { into("common") }
    from("libraries/stdlib/test") { into("test") }
    from("libraries/kotlin.test/common/src/test/kotlin") { into("kotlin-test") }
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

fun Project.secureZipTask(zipTask: TaskProvider<Zip>): RegisteringDomainObjectDelegateProviderWithAction<out TaskContainer, Task> {
    val checkSumTask = tasks.register("${zipTask.name}Checksum", Checksum::class) {
        dependsOn(zipTask)
        val compilerFile = zipTask.get().outputs.files.singleFile
        files = files(compilerFile)
        outputDir = compilerFile.parentFile
        algorithm = Checksum.Algorithm.SHA256
    }

    val signTask = tasks.register("${zipTask.name}Sign", Sign::class) {
        description = "Signs the archive produced by the '" + zipTask.name + "' task."
        sign(zipTask.get())
    }

    return tasks.registering {
        dependsOn(checkSumTask)
        dependsOn(signTask)
    }
}

signing {
    useGpgCmd()
}

val zipCompilerWithSignature by secureZipTask(zipCompiler)

configure<IdeaModel> {
    module {
        excludeDirs = files(
            project.buildDir,
            commonLocalDataDir,
            ".gradle",
            "dependencies",
            "dist",
            "tmp",
            "intellij"
        ).toSet()
    }
}

val disableVerificationTasks = providers.gradleProperty("kotlin.build.disable.verification.tasks")
    .forUseAtConfigurationTime().orNull?.toBoolean() ?: false
if (disableVerificationTasks) {
    logger.info("Verification tasks are disabled because `kotlin.build.disable.verification.tasks` is true")
    gradle.taskGraph.whenReady {
        allTasks.forEach {
            if (it is VerificationTask) {
                logger.info("Task ${it.path} is disabled because `kotlin.build.disable.verification.tasks` is true")
                it.enabled = false
            }
        }
    }
}

gradle.taskGraph.whenReady(checkYarnAndNPMSuppressed)

plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class) {
    extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java) {
        npmInstallTaskProvider?.configure {
            args += listOf("--network-concurrency", "1", "--mutex", "network")
        } ?: error("kotlinNpmInstall task should exist inside NodeJsRootExtension")
    }
}

afterEvaluate {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
    if (cacheRedirectorEnabled) {
        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().downloadBaseUrl =
                "https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download"
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockMismatchReport =
                YarnLockMismatchReport.WARNING
        }
    }
}

afterEvaluate {
    checkExpectedGradlePropertyValues()
}
