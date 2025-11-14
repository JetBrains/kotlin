
plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(kotlinStdlib("jdk8"))
    testFixturesApi(project(":kotlin-scripting-compiler"))
    testFixturesApi(project(":core:descriptors"))
    testFixturesApi(project(":core:descriptors.jvm"))
    testFixturesApi(project(":core:deserialization"))
    testFixturesApi(project(":compiler:util"))
    testFixturesApi(project(":compiler:tests-mutes"))
    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:ir.tree"))
    testFixturesApi(project(":compiler:fir:tree"))
    testFixturesApi(project(":compiler:fir:raw-fir:psi2fir"))
    testFixturesApi(project(":compiler:fir:raw-fir:light-tree2fir"))
    testFixturesApi(project(":compiler:fir:fir2ir"))
    testFixturesApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testFixturesApi(project(":compiler:fir:fir-serialization"))
    testFixturesApi(project(":compiler:fir:fir-deserialization"))
    testFixturesApi(project(":compiler:fir:cones"))
    testFixturesApi(project(":compiler:fir:resolve"))
    testFixturesApi(project(":compiler:fir:providers"))
    testFixturesApi(project(":compiler:fir:semantics"))
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.js"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.native"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.wasm"))
    testFixturesApi(project(":compiler:fir:fir-jvm"))
    testFixturesApi(project(":compiler:fir:fir-js"))
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":compiler:frontend.java"))
    testFixturesApi(project(":compiler:util"))
    testFixturesApi(project(":compiler:cli-common"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:cli-js"))
    testFixturesApi(project(":analysis:light-classes-base"))
    testFixturesApi(project(":compiler:serialization"))
    testFixturesApi(project(":kotlin-preloader"))
    testFixturesApi(project(":compiler:cli-common"))
    testFixturesApi(project(":daemon-common"))
    testFixturesApi(project(":js:js.frontend"))
    testFixturesApi(project(":native:frontend.native"))
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(kotlinTest())
    testFixturesApi(project(":kotlin-scripting-compiler-impl"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(libs.junit4) // for ComparisonFailure
    testFixturesApi(commonDependency("com.android.tools:r8"))
    testFixturesApi(project(":analysis:analysis-internal-utils"))
    testFixturesApi(project(":compiler:tests-mutes:mutes-junit4"))
    testFixturesCompileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesCompileOnly(toolsJarApi())
    testFixturesCompileOnly(intellijCore())

    /*
     * Actually those dependencies are needed only at runtime, but they
     *   declared as Api dependencies to propagate them to all modules
     *   which depend on current one
     */
    testFixturesApi(libs.intellij.fastutil)
    testFixturesApi(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testFixturesApi(commonDependency("one.util:streamex"))
    testFixturesApi(commonDependency("org.codehaus.woodstox:stax2-api"))
    testFixturesApi(commonDependency("com.fasterxml:aalto-xml"))
    testFixturesApi(libs.opentest4j)

    testFixturesApi(jpsModel()) { isTransitive = false }
    testFixturesApi(jpsModelImpl()) { isTransitive = false }

    testFixturesImplementation(libs.guava)
    testFixturesImplementation(libs.intellij.asm)
    testFixturesImplementation(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testFixturesImplementation(intellijJDom())

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.processTestFixturesResources.configure {
    into("legacy") {
        from(project(":compiler").layout.projectDirectory.dir("testData")) {
            include("/diagnostics/helpers/types/checkTypeWithExact.kt")
        }
    }
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()
optInToK1Deprecation()

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

testsJar {}
