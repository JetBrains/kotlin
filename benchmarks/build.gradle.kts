import kotlinx.benchmark.gradle.benchmark

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    api(kotlinStdlib())
    api(testFixtures(project(":compiler:tests-common")))
    api(project(":compiler:cli"))
    api(intellijCore())
    api(libs.kotlinx.benchmark.runtime)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

benchmark {
    configurations {
        named("main") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "sec"
            param("size", 1000)

            include("CommonCallsBenchmark")
            include("ControlFlowAnalysisBenchmark")

            /*include("InferenceBaselineCallsBenchmark")
            include("InferenceExplicitArgumentsCallsBenchmark")
            include("InferenceForInApplicableCandidate")
            include("InferenceFromArgumentCallsBenchmark")
            include("InferenceFromReturnTypeCallsBenchmark")*/
        }
    }
    targets {
        register("test")
    }
}

tasks.withType<JavaExec>().matching { it.name == "testBenchmark" }.configureEach {
    dependsOn(":createIdeaHomeForTests")
    systemProperty("idea.home.path", ideaHomePathForTests().get().asFile.canonicalPath)
    systemProperty("idea.use.native.fs.for.win", false)
}
