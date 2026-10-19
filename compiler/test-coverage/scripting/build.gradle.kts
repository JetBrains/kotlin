import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("base")
    id("jacoco")
}

description = "Aggregated JaCoCo coverage for compiler code exercised by Kotlin scripting tests"

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val scriptingTestProjects = listOf(
    ":plugins:scripting:scripting-tests",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-jvm-host-test",
    ":kotlin-scripting-dependencies",
    ":kotlin-scripting-dependencies-maven",
    ":kotlin-scripting-jsr223-test",
    ":kotlin-main-kts",
    ":kotlin-main-kts-test",
    ":examples:scripting-jvm-simple-script-host",
    ":examples:scripting-jvm-maven-deps-host",
    ":examples:scripting-jvm-embeddable-host",
    ":examples:scripting-jsr223-daemon",
    ":examples:scripting-jsr223-bta",
)

// These projects use Kotlin's custom projectTests/Test tasks rather than published
// JVM-test-suite coverage variants. Evaluate them first so the task collection below
// sees every custom Test task, then apply JaCoCo directly and consume each task's
// configured destination file instead of relying on jacocoAggregation resolution.
scriptingTestProjects.forEach { evaluationDependsOn(it) }

val scriptingExecutionData = objects.fileCollection()
scriptingTestProjects.forEach { path ->
    val producer = project(path)
    producer.pluginManager.apply("jacoco")
    producer.tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            isEnabled = true
            scriptingExecutionData.from(destinationFile)
        }
    }
}


// Class/source providers: these are report inputs only, never execution-data producers.
val reportClassScopeProjects = listOf(
    // The compiler-side scripting implementation is here, not in scripting-tests.
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-dependencies",
    ":kotlin-scripting-dependencies-maven",
    ":kotlin-main-kts",
    ":examples:scripting-jvm-simple-script-host",
    ":examples:scripting-jvm-maven-deps-host",
    ":examples:scripting-jvm-embeddable-host",
    ":examples:scripting-jsr223-daemon",
    ":examples:scripting-jsr223-bta",
    ":compiler",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":compiler:arguments.common",
    ":compiler:arguments",
    ":compiler:container",
    ":compiler:resolution.common",
    ":compiler:resolution.common.js",
    ":compiler:resolution.common.jvm",
    ":compiler:resolution",
    ":compiler:serialization.common",
    ":compiler:serialization",
    ":compiler:psi:psi-api",
    ":compiler:psi:psi-impl",
    ":compiler:psi:psi-utils",
    ":compiler:psi:psi-frontend-utils",
    ":compiler:psi:parser",
    ":compiler:multiplatform-parsing",
    ":compiler:frontend",
    ":compiler:frontend.common",
    ":compiler:frontend.common-psi",
    ":compiler:frontend.java",
    ":compiler:frontend.common.jvm",
    ":compiler:frontend:cfg",
    ":compiler:ir.tree",
    ":compiler:ir.psi2ir",
    ":compiler:ir.objcinterop",
    ":compiler:ir.backend.common",
    ":compiler:ir.backend.native",
    ":compiler:ir.actualization",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.serialization.js",
    ":compiler:ir.serialization.native",
    ":compiler:ir.serialization.jklib",
    ":compiler:ir.inline",
    ":compiler:ir.validation",
    ":compiler:backend",
    ":compiler:backend.common.jvm",
    ":compiler:backend.jvm",
    ":compiler:backend.jvm.lower",
    ":compiler:backend.jvm.codegen",
    ":compiler:backend.jvm.entrypoint",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":compiler:plugin-api",
    ":compiler:java-direct",
    ":compiler:cli-base",
    ":compiler:cli",
    ":compiler:cli-jvm",
    ":compiler:cli-js",
    ":compiler:cli-jklib",
    ":compiler:cli-metadata",
    ":compiler:cli:cli-native-klib",
    ":compiler:incremental-compilation-impl",
    ":compiler:fir:cones",
    ":compiler:fir:tree",
    ":compiler:fir:semantics.api",
    ":compiler:fir:resolve",
    ":compiler:fir:plugin.api",
    ":compiler:fir:fir-serialization",
    ":compiler:fir:fir-deserialization",
    ":compiler:fir:fir-jvm",
    ":compiler:fir:fir-native",
    ":compiler:fir:checkers:checkers.api",
    ":compiler:fir:checkers:checkers.common",
    ":compiler:fir:checkers:checkers.jvm",
    ":compiler:fir:checkers:checkers.js",
    ":compiler:fir:checkers:checkers.native",
    ":compiler:fir:checkers:checkers.wasm",
    ":compiler:fir:checkers:checkers.web.common",
    ":compiler:fir:diagnostic-renderers",
    ":compiler:fir:entrypoint",
    ":compiler:fir:raw-fir:raw-fir.common",
    ":compiler:fir:fir2ir",
    ":compiler:fir:fir2ir:jvm-backend",
    ":core:names",
    ":core:language.model",
    ":core:language.targets",
    ":core:language.targets.jvm",
    ":core:language.version-settings",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:compiler.common.native",
    ":core:compiler.common.wasm",
    ":core:compiler.common.web",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":core:deserialization",
    ":core:deserialization.common",
    ":core:deserialization.common.jvm",
    ":core:descriptors.runtime",
    ":core:metadata",
    ":core:metadata.jvm",
    ":core:util.runtime",
)

val reportJvmClassScopeProjects = reportClassScopeProjects.filter { path ->
    val candidate = project(path)
    candidate.extensions.findByType<SourceSetContainer>()?.findByName("main") != null &&
        candidate.tasks.findByName("classes") != null
}

val reportClassDirectories = objects.fileCollection()
val reportSourceDirectories = objects.fileCollection()

reportJvmClassScopeProjects.forEach { path ->
    val mainSourceSet = project(path).extensions.getByType<SourceSetContainer>().findByName("main")!!
    reportClassDirectories.from(mainSourceSet.output.classesDirs)
    reportSourceDirectories.from(mainSourceSet.allSource.srcDirs)
}

tasks.register<JacocoReport>("scriptingCoverageReport") {
    group = "verification"
    description = "Generates compiler-wide coverage from scripting tests only"

    // Run every scripting producer before evaluating the report's built-in
    // "any execution data exists" predicate.
    scriptingTestProjects.forEach { path ->
        dependsOn(project(path).tasks.withType<Test>())
    }
    executionData.setFrom(scriptingExecutionData)

    // Build class-only inputs. Test projects contribute execution data only.
    dependsOn(reportJvmClassScopeProjects.map { "$it:classes" })
    classDirectories.setFrom(
        reportClassDirectories.asFileTree.matching {
            include(
                // Compiler/plugin-side scripting implementation and JSR-223 integration.
                "org/jetbrains/kotlin/**",
                // Public scripting API/host implementations use this package root.
                "kotlin/script/experimental/**"
            )
            exclude("org/jetbrains/kotlin/util/vavr/VavrAdaptersKt.class")
        }
    )
    sourceDirectories.setFrom(reportSourceDirectories)

    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }

}
