
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(kotlinStdlib("jdk8"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":core:descriptors"))
    testApi(project(":core:descriptors.jvm"))
    testApi(project(":core:deserialization"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:tests-mutes"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:ir.tree"))
    testApi(project(":compiler:fir:tree"))
    testApi(project(":compiler:fir:raw-fir:psi2fir"))
    testApi(project(":compiler:fir:raw-fir:light-tree2fir"))
    testApi(project(":compiler:fir:fir2ir"))
    testApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testApi(project(":compiler:fir:fir-serialization"))
    testApi(project(":compiler:fir:fir-deserialization"))
    testApi(project(":compiler:fir:cones"))
    testApi(project(":compiler:fir:resolve"))
    testApi(project(":compiler:fir:providers"))
    testApi(project(":compiler:fir:semantics"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testApi(project(":compiler:fir:checkers:checkers.js"))
    testApi(project(":compiler:fir:checkers:checkers.native"))
    testApi(project(":compiler:fir:checkers:checkers.wasm"))
    testApi(project(":compiler:fir:java"))
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:frontend.java"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:cli-js"))
    testApi(project(":analysis:light-classes-base"))
    testApi(project(":compiler:serialization"))
    testApi(project(":kotlin-preloader"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":daemon-common"))
    testApi(project(":js:js.frontend"))
    testApi(project(":native:frontend.native"))
    testCompileOnly(project(":plugins:android-extensions-compiler"))
    testApi(projectTests(":generators:test-generator"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(kotlinTest())
    testApi(project(":kotlin-scripting-compiler-impl"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(libs.junit4) // for ComparisonFailure
    testApi(commonDependency("com.android.tools:r8"))
    testApi(project(":analysis:analysis-internal-utils"))
    testApi(project(":compiler:tests-mutes:mutes-junit4"))
    testCompileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testCompileOnly(toolsJarApi())
    testCompileOnly(intellijCore())

    /*
     * Actually those dependencies are needed only at runtime, but they
     *   declared as Api dependencies to propagate them to all modules
     *   which depend on current one
     */
    testApi(libs.intellij.fastutil)
    testApi(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testApi(commonDependency("one.util:streamex"))
    testApi(commonDependency("org.codehaus.woodstox:stax2-api"))
    testApi(commonDependency("com.fasterxml:aalto-xml"))
    testApi(libs.opentest4j)

    testApi(jpsModel()) { isTransitive = false }
    testApi(jpsModelImpl()) { isTransitive = false }

    testImplementation(libs.guava)
    testImplementation(libs.intellij.asm)
    testImplementation(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testImplementation(intellijJDom())

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
