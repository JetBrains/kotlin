import org.gradle.crypto.checksum.Checksum
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
    // a workaround for kotlin compiler classpath in kotlin project: sometimes gradle substitutes
    // kotlin-stdlib external dependency with local project :kotlin-stdlib in kotlinCompilerClasspath configuration.
    // see also configureCompilerClasspath@
    val bootstrapCompilerClasspath by configurations.creating
    val bootstrapBuildToolsApiClasspath by configurations.creating

    dependencies {
        bootstrapCompilerClasspath(kotlin("compiler-embeddable", bootstrapKotlinVersion))
        bootstrapBuildToolsApiClasspath(kotlin("build-tools-impl", bootstrapKotlinVersion))

        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    }

    val gsonVersion = libs.versions.gson.get()
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion(gsonVersion)
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }
}

plugins {
    base
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.0.1" // this version should be in sync with repo/buildsrc-compat/build.gradle.kts
    id("build-time-report")
    id("java-instrumentation")
    id("jps")
    id("modularized-test-configurations")
    id("idea-rt-hack")
    id("resolve-dependencies")
    id("org.gradle.crypto.checksum") version "1.4.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.1" apply false
    signing
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        id("kotlin.native.build-tools-conventions") apply false
    }
    `jvm-toolchains`
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

val defaultSnapshotVersion: String by extra
findProperty("deployVersion")?.let {
    assert(findProperty("build.number") != null) { "`build.number` parameter is expected to be explicitly set with the `deployVersion`" }
}
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(
    findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
        if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else defaultSnapshotVersion
    } ?: buildNumber
)

val kotlinLanguageVersion: String by extra
val kotlinApiVersionForModulesUsedInIDE: String by extra

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

// TODO: use "by extra()" syntax where possible
extra["distLibDir"] = project.file(distLibDir)
extra["commonLocalDataDir"] = project.file(commonLocalDataDir)
extra["ideaSandboxDir"] = project.file(ideaSandboxDir)
extra["ideaPluginDir"] = project.file(ideaPluginDir)

rootProject.apply {
    from(rootProject.file("gradle/versions.gradle.kts"))
    from(rootProject.file("gradle/checkArtifacts.gradle.kts"))
    from(rootProject.file("gradle/checkCacheability.gradle.kts"))
    from(rootProject.file("gradle/retryPublishing.gradle.kts"))
}

IdeVersionConfigurator.setCurrentIde(project)

if (!project.hasProperty("versions.kotlin-native")) {
    // BEWARE! Bumping this version doesn't take an immediate effect on TeamCity: KTI-1107
    extra["versions.kotlin-native"] = "2.0.0-dev-13302"
}

val irCompilerModules = arrayOf(
    ":compiler:ir.tree",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.js",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.serialization.native",
    ":compiler:ir.objcinterop",
    ":compiler:ir.backend.common",
    ":compiler:ir.actualization",
    ":compiler:ir.interpreter",
    ":compiler:ir.inline",
    ":wasm:wasm.ir"
).also { extra["irCompilerModules"] = it }

val irCompilerModulesForIDE = arrayOf(
    ":compiler:ir.tree",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.serialization.js", // used in IJ android plugin in `ComposeIrGenerationExtension`
    ":compiler:ir.objcinterop",
    ":compiler:ir.backend.common",
    ":compiler:ir.actualization",
    ":compiler:ir.interpreter",
    ":compiler:ir.inline",
).also { extra["irCompilerModulesForIDE"] = it }

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
    ":core:compiler.common.native",
    ":core:compiler.common.wasm",
    ":core:compiler.common.web",
    ":core:util.runtime",
    ":compiler:frontend.common.jvm",
    ":compiler:frontend.java", // TODO this is fe10 module but some utils used in fir ide now
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-js",
    ":analysis:decompiled:decompiler-native",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":analysis:analysis-api-providers",
    ":analysis:project-structure",
    ":analysis:kt-references",
    ":kotlin-build-common",
    ":compiler:build-tools:kotlin-build-statistics",
    ":compiler:build-tools:kotlin-build-tools-api",
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
    ":compiler:fir:native",
    ":compiler:fir:raw-fir:raw-fir.common",
    ":compiler:fir:raw-fir:psi2fir",
    ":compiler:fir:checkers",
    ":compiler:fir:checkers:checkers.jvm",
    ":compiler:fir:checkers:checkers.js",
    ":compiler:fir:checkers:checkers.native",
    ":compiler:fir:checkers:checkers.wasm",
    ":compiler:fir:checkers:checkers.web.common",
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
    ":kotlin-util-klib-abi",
    ":compiler:backend-common",
    ":compiler:backend",
    ":compiler:plugin-api",
    ":compiler:javac-wrapper",
    ":compiler:cli-common",
    ":compiler:cli-base",
    ":compiler:cli",
    ":compiler:cli-js",
    ":compiler:incremental-compilation-impl",
    ":compiler:compiler.version",
    ":js:js.ast",
    ":js:js.sourcemap",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.config",
    ":js:js.frontend",
    ":js:js.translator",
    ":js:js.dce",
    ":native:frontend.native",
    ":native:kotlin-native-utils",
    ":wasm:wasm.frontend",
    ":wasm:wasm.config",
    ":compiler:backend.common.jvm",
    ":analysis:decompiled:light-classes-for-decompiled-fe10",
).also { extra["fe10CompilerModules"] = it }

extra["compilerModules"] =
    irCompilerModules +
            fe10CompilerModules +
            commonCompilerModules +
            firAllCompilerModules

/**
 * An array of projects used in the IntelliJ Kotlin Plugin.
 *
 * Experimental declarations from Kotlin stdlib cannot be used in those projects to avoid stdlib binary compatibility problems.
 * See KT-62510 for details.
 */
val projectsUsedInIntelliJKotlinPlugin =
    fe10CompilerModules +
            commonCompilerModules +
            firCompilerCoreModules +
            irCompilerModulesForIDE +
            arrayOf(
                ":analysis:analysis-api",
                ":analysis:analysis-api-fe10",
                ":analysis:analysis-api-fir",
                ":analysis:analysis-api-impl-barebone",
                ":analysis:analysis-api-impl-base",
                ":analysis:analysis-api-providers",
                ":analysis:analysis-api-standalone:analysis-api-standalone-base",
                ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
                ":analysis:analysis-api-standalone",
                ":analysis:analysis-internal-utils",
                ":analysis:analysis-test-framework",
                ":analysis:decompiled",
                ":analysis:kt-references",
                ":analysis:kt-references:kt-references-fe10",
                ":analysis:light-classes-base",
                ":analysis:low-level-api-fir",
                ":analysis:project-structure",
                ":analysis:symbol-light-classes",
            ) +
            arrayOf(
                ":kotlin-allopen-compiler-plugin.cli",
                ":kotlin-allopen-compiler-plugin.common",
                ":kotlin-allopen-compiler-plugin.k1",
                ":kotlin-allopen-compiler-plugin.k2",

                ":kotlin-assignment-compiler-plugin.cli",
                ":kotlin-assignment-compiler-plugin.common",
                ":kotlin-assignment-compiler-plugin.k1",
                ":kotlin-assignment-compiler-plugin.k2",

                ":plugins:parcelize:parcelize-compiler:parcelize.backend",
                ":plugins:parcelize:parcelize-compiler:parcelize.cli",
                ":plugins:parcelize:parcelize-compiler:parcelize.common",
                ":plugins:parcelize:parcelize-compiler:parcelize.k1",
                ":plugins:parcelize:parcelize-compiler:parcelize.k2",
                ":plugins:parcelize:parcelize-runtime",

                ":kotlin-sam-with-receiver-compiler-plugin.cli",
                ":kotlin-sam-with-receiver-compiler-plugin.common",
                ":kotlin-sam-with-receiver-compiler-plugin.k1",
                ":kotlin-sam-with-receiver-compiler-plugin.k2",

                ":kotlinx-serialization-compiler-plugin.cli",
                ":kotlinx-serialization-compiler-plugin.common",
                ":kotlinx-serialization-compiler-plugin.k1",
                ":kotlinx-serialization-compiler-plugin.k2",
                ":kotlinx-serialization-compiler-plugin.backend",

                ":kotlin-lombok-compiler-plugin.cli",
                ":kotlin-lombok-compiler-plugin.common",
                ":kotlin-lombok-compiler-plugin.k1",
                ":kotlin-lombok-compiler-plugin.k2",

                ":kotlin-noarg-compiler-plugin.cli",
                ":kotlin-noarg-compiler-plugin.common",
                ":kotlin-noarg-compiler-plugin.k1",
                ":kotlin-noarg-compiler-plugin.k2",
                ":kotlin-noarg-compiler-plugin.backend",

                ":plugins:android-extensions-compiler",

                ":kotlin-sam-with-receiver-compiler-plugin.cli",
                ":kotlin-sam-with-receiver-compiler-plugin.common",
                ":kotlin-sam-with-receiver-compiler-plugin.k1",
                ":kotlin-sam-with-receiver-compiler-plugin.k2",


                ":kotlin-compiler-runner-unshaded",
                ":kotlin-preloader",
                ":daemon-common",
                ":kotlin-daemon-client",

                ":kotlin-scripting-compiler",
                ":kotlin-gradle-statistics",
                ":jps:jps-common",
            ) +
            arrayOf(
                ":native:base",
                ":native:objcexport-header-generator",
                ":native:objcexport-header-generator-k1",
                ":native:objcexport-header-generator-analysis-api",
                ":compiler:ir.serialization.native"
            )

extra["projectsUsedInIntelliJKotlinPlugin"] = projectsUsedInIntelliJKotlinPlugin

// They are embedded just because we don't publish those dependencies as separate Maven artifacts (yet)
extra["kotlinJpsPluginEmbeddedDependencies"] = listOf(
    ":compiler:cli-common",
    ":kotlin-build-tools-enum-compat",
    ":kotlin-compiler-runner-unshaded",
    ":daemon-common",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:compiler.common.native",
    ":core:compiler.common.wasm",
    ":core:compiler.common.web",
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
    ":js:js.config",
    ":wasm:wasm.config",
    ":core:util.runtime",
    ":compiler:compiler.version",
    ":compiler:build-tools:kotlin-build-statistics",
)

extra["kotlinJpsPluginMavenDependencies"] = listOf(
    ":kotlin-daemon-client",
    ":kotlin-build-common",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":native:kotlin-native-utils",
    ":compiler:build-tools:kotlin-build-tools-api",
)

extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"] = listOf(
    commonDependency("org.jetbrains.kotlin:kotlin-reflect")
)

extra["compilerArtifactsForIde"] = listOfNotNull(
    ":prepare:ide-plugin-dependencies:android-extensions-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:scripting-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide",
    ":prepare:ide-plugin-dependencies:js-ir-runtime-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide",
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
    ":prepare:ide-plugin-dependencies:kotlin-objcexport-header-generator-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-testdata-for-ide",
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
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-android-extensions-runtime",
    ":plugins:parcelize:parcelize-runtime",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-test",
    ":kotlin-daemon",
    ":kotlin-compiler",
    ":kotlin-annotations-jvm",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-reflect",
    ":kotlin-main-kts",
    ":kotlin-dom-api-compat"
)

val coreLibProjects by extra {
    listOfNotNull(
        ":kotlin-stdlib",
        ":kotlin-stdlib-common",
        ":kotlin-stdlib-jdk7",
        ":kotlin-stdlib-jdk8",
        ":kotlin-test",
        ":kotlin-reflect"
    )
}
val mppProjects by extra {
    listOf(
        ":kotlin-stdlib",
        ":kotlin-test",
    )
}

val projectsWithEnabledContextReceivers by extra {
    listOf(
        ":core:descriptors.jvm",
        ":compiler:resolution.common",
        ":compiler:frontend.common",
        ":compiler:fir:resolve",
        ":compiler:fir:plugin-utils",
        ":compiler:fir:fir2ir",
        ":compiler:fir:raw-fir:raw-fir.common",
        ":compiler:fir:raw-fir:psi2fir",
        ":compiler:fir:raw-fir:light-tree2fir",
        ":compiler:fir:tree:tree-generator",
        ":compiler:ir.tree:tree-generator",
        ":native:swift:sir:tree-generator",
        ":generators:tree-generator-common",
        ":kotlin-lombok-compiler-plugin.k1",
        ":kotlinx-serialization-compiler-plugin.k2",
        ":plugins:parcelize:parcelize-compiler:parcelize.k2",
        ":plugins:fir-plugin-prototype",
        ":plugins:kapt4",
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
    ":kotlin-gradle-plugin-model",
    ":kotlin-gradle-plugin-tcs-android",
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-power-assert",
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
            if (project.path in @Suppress("UNCHECKED_CAST") (rootProject.extra["compilerModules"] as Array<String>)) {
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
            }
        }
    }
}

preparePublication()

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
        with(ideaBuildNumberFileForTests.get().asFile) {
            parentFile.mkdirs()
            writeText("IC-$intellijSdkVersion")
        }
    }
}

tasks {
    named<Delete>("clean") {
        delete(distDir)
        delete(layout.buildDirectory.dir("repo"))
    }

    register<Delete>("cleanupArtifacts") {
        delete = setOf(artifactsDir)
    }

    listOf("clean", "assemble", "install", "publish").forEach { taskName ->
        register("coreLibs${taskName.capitalize()}") {
            for (projectName in coreLibProjects) {
                if (projectName.startsWith(":kotlin-test:") && (taskName == "install" || taskName == "publish")) continue
                dependsOn("$projectName:$taskName")
            }
        }
    }

    register("coreLibsTest") {
        (coreLibProjects + listOfNotNull(
            ":kotlin-stdlib:samples",
            ":kotlin-test:kotlin-test-js-it".takeIf { !kotlinBuildProperties.isInJpsBuildIdeaSync },
            ":kotlin-metadata-jvm",
            ":tools:binary-compatibility-validator",
            ":tools:jdk-api-validator",
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
        dependsOn(":wasm:wasm.tests:testK1")
        dependsOn(":wasm:wasm.tests:diagnosticTest")
        // Windows WABT release requires Visual C++ Redistributable
        if (!kotlinBuildProperties.isTeamcityBuild || !org.gradle.internal.os.OperatingSystem.current().isWindows) {
            dependsOn(":wasm:wasm.ir:test")
        }
    }

    register("wasmFirCompilerTest") {
        dependsOn(":wasm:wasm.tests:testFir")
    }

    // These tests run Native compiler and will be run in many different compilation modes that the compiler supports:
    // - different optimization modes
    // - different cache policies
    // - different GCs
    // ...
    register("nativeCompilerTest") {
        dependsOn(":native:native.tests:test")
        dependsOn(":native:objcexport-header-generator:check")
        dependsOn(":kotlin-atomicfu-compiler-plugin:nativeTest")
    }

    // These are unit tests of Native compiler
    register("nativeCompilerUnitTest") {
        dependsOn(":native:kotlin-native-utils:check")
        if (kotlinBuildProperties.isKotlinNativeEnabled) {
            dependsOn(":kotlin-native:Interop:Indexer:check")
            dependsOn(":kotlin-native:Interop:StubGenerator:check")
            dependsOn(":kotlin-native:backend.native:check")
        }
    }

    register("firCompilerTest") {
        dependsOn(":compiler:fir:raw-fir:psi2fir:test")
        dependsOn(":compiler:fir:raw-fir:light-tree2fir:test")
        dependsOn(":compiler:fir:analysis-tests:test")
        dependsOn(":compiler:fir:analysis-tests:legacy-fir-tests:test")
        dependsOn(":compiler:fir:fir2ir:aggregateTests")
    }

    register("nightlyFirCompilerTest") {
        dependsOn(":compiler:fir:fir2ir:nightlyTests")
    }

    register("scriptingJvmTest") {
        dependsOn("dist")
        dependsOn(":kotlin-scripting-compiler:test")
        dependsOn(":kotlin-scripting-common:test")
        dependsOn(":kotlin-scripting-jvm:test")
        dependsOn(":kotlin-scripting-jvm-host-test:test")
        dependsOn(":kotlin-scripting-dependencies:test")
        dependsOn(":kotlin-scripting-dependencies-maven:test")
        dependsOn(":kotlin-scripting-dependencies-maven-all:test")
        // see comments on the task in kotlin-scripting-jvm-host-test
//        dependsOn(":kotlin-scripting-jvm-host-test:embeddableTest")
        dependsOn(":kotlin-main-kts-test:test")
    }

    register("scriptingK1JvmTest") {
        dependsOn(":kotlin-scripting-compiler:testWithK1")
        dependsOn(":kotlin-scripting-jvm-host-test:testWithK1")
        dependsOn(":kotlin-main-kts-test:testWithK1")
        dependsOn(":kotlin-scripting-jsr223-test:test")
        dependsOn(":kotlin-scripting-jsr223-test:embeddableTest")
        dependsOn(":kotlin-scripting-ide-services-test:test")
        dependsOn(":kotlin-scripting-ide-services-test:embeddableTest")
    }

    register("scriptingTest") {
        dependsOn("scriptingJvmTest")
        dependsOn("scriptingK1JvmTest")
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
        dependsOn(":kotlin-util-klib-abi:test")
        dependsOn(":generators:test")
    }

    register("incrementalCompilationTest") {
        dependsOn(":compiler:incremental-compilation-impl:test")
        dependsOn(":compiler:incremental-compilation-impl:testJvmICWithJdk11")
    }

    register("compilerPluginTest") {
        dependsOn(":kotlin-allopen-compiler-plugin:test")
        dependsOn(":kotlin-assignment-compiler-plugin:test")
        dependsOn(":kotlin-atomicfu-compiler-plugin:test")
        dependsOn(":plugins:fir-plugin-prototype:test")
        dependsOn(":plugins:fir-plugin-prototype:fir-plugin-ic-test:test")
        dependsOn(":kotlin-imports-dumper-compiler-plugin:test")
        dependsOn(":plugins:jvm-abi-gen:test")
        dependsOn(":plugins:js-plain-objects:compiler-plugin:test")
        dependsOn(":kotlinx-serialization-compiler-plugin:test")
        dependsOn(":kotlin-lombok-compiler-plugin:test")
        dependsOn(":kotlin-noarg-compiler-plugin:test")
        dependsOn(":kotlin-sam-with-receiver-compiler-plugin:test")
        dependsOn(":kotlin-power-assert-compiler-plugin:test")
    }

    register("toolsTest") {
        dependsOn(":tools:kotlinp-jvm:test")
        dependsOn(":native:kotlin-klib-commonizer:test")
        dependsOn(":native:kotlin-klib-commonizer-api:test")
        dependsOn(":kotlin-tooling-core:check")
        dependsOn(":kotlin-tooling-metadata:check")
        dependsOn(":compiler:build-tools:kotlin-build-tools-api:check")
        dependsOn(":tools:ide-plugin-dependencies-validator:test")
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
        dependsOn(":kotlin-annotation-processing-compiler:test")
        dependsOn(":kotlin-annotation-processing-compiler:testJdk11")
        dependsOn(":kotlin-annotation-processing-base:test")
        dependsOn(":kotlin-annotation-processing-cli:test")
    }

    register("parcelizeTests") {
        dependsOn(":plugins:parcelize:parcelize-compiler:test")
    }

    register("codebaseTests") {
        dependsOn(":repo:codebase-tests:test")
    }

    register("statisticsTests") {
        dependsOn(":kotlin-gradle-statistics:test")
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
            val bootstrapKotlinVersion = bootstrapKotlinVersion
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

    register<Exec>("mvnInstall") {
        group = "publishing"
        workingDir = rootProject.projectDir.resolve("libraries")
        commandLine = getMvnwCmd() + listOf("clean", "install", "-DskipTests")
        doFirst {
            environment("JDK_1_8", getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8).get())
        }
    }
    register<Exec>("mvnPublish") {
        group = "publishing"
        workingDir = rootProject.projectDir.resolve("libraries")
        commandLine = getMvnwCmd() + listOf(
            "clean", "deploy", "--activate-profiles=noTest",
            "-Dinvoker.skip=true", "-DskipTests",
            "-Ddeploy-snapshot-repo=local",
            "-Ddeploy-snapshot-url=file://${rootProject.projectDir.resolve("build/repo")}"
        )
        doFirst {
            environment("JDK_1_8", getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8).get())
        }
    }
}

val zipCompiler by tasks.registering(Zip::class) {
    dependsOn(dist)
    destinationDirectory.set(file(distDir))
    archiveFileName.set("kotlin-compiler-$kotlinVersion.zip")

    from(distKotlinHomeDir)
    into("kotlinc")

    doLast {
        logger.lifecycle("Compiler artifacts packed to ${archiveFile.get().asFile.absolutePath}")
    }
}

fun Project.secureZipTask(zipTask: TaskProvider<Zip>): RegisteringDomainObjectDelegateProviderWithAction<out TaskContainer, Task> {
    val checkSumTask: TaskProvider<Checksum> = tasks.register("${zipTask.name}Checksum", Checksum::class) {
        dependsOn(zipTask)
        inputFiles.setFrom(zipTask.map { it.outputs.files.singleFile })
        outputDirectory.fileProvider(zipTask.map { it.outputs.files.singleFile.parentFile.resolve("checksums") })
        checksumAlgorithm.set(Checksum.Algorithm.SHA256)
    }

    // Don't use Copy task, because it declares the full destination directory as an output
    val copyChecksumTask = tasks.register("${zipTask.name}ChecksumCopy") {
        dependsOn(checkSumTask)

        val checksumFileName: Provider<String> = zipTask.map { "${it.outputs.files.singleFile.name}.sha256" }
        val checksumFile: Provider<RegularFile> = checkSumTask.map { it.outputDirectory.file(checksumFileName.get()).get() }
        val outputFile: Provider<File> = zipTask.map { it.outputs.files.singleFile.parentFile.resolve(checksumFileName.get()) }

        inputs.file(checksumFile)
        outputs.file(outputFile)

        doLast {
            checksumFile.get().asFile.copyTo(outputFile.get(), overwrite = true)
        }
    }

    val signTask = tasks.register("${zipTask.name}Sign", Sign::class) {
        description = "Signs the archive produced by the '" + zipTask.name + "' task."
        sign(zipTask.get())
    }

    return tasks.registering {
        dependsOn(copyChecksumTask)
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
            project.layout.buildDirectory,
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
    .orNull?.toBoolean() ?: false
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

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class) {
    extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java) {
        // Node.js with canary v8 that supports recent Wasm GC changes
        version = "21.0.0-v8-canary20231019bd785be450"
        downloadBaseUrl = if (cacheRedirectorEnabled)
            "https://cache-redirector.jetbrains.com/nodejs.org/download/v8-canary"
        else
            "https://nodejs.org/download/v8-canary"

        npmInstallTaskProvider.configure {
            args += listOf("--network-concurrency", "1", "--mutex", "network")
            // It's required to pass compatibility checks in some NPM packages while using Node.js with Canary v8.
            // TODO remove as soon as we switch to release build of Node.js.
            args += listOf("--ignore-engines")
        }
    }
}

afterEvaluate {
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
