
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
    testApi(project(":compiler:ir.tree.impl"))
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
    testApi(project(":compiler:fir:java"))
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:ir.ir2cfg"))
    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:frontend.java"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:cli-js"))
    testApi(project(":compiler:light-classes"))
    testApi(project(":compiler:serialization"))
    testApi(project(":kotlin-preloader"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":daemon-common"))
    testApi(project(":daemon-common-new"))
    testApi(project(":js:js.serializer"))
    testApi(project(":js:js.frontend"))
    testApi(project(":js:js.translator"))
    testApi(project(":native:frontend.native"))
    testCompileOnly(project(":plugins:android-extensions-compiler"))
    testApi(projectTests(":generators:test-generator"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(projectTests(":compiler:tests-common-jvm6"))
    testApi(project(":kotlin-scripting-compiler-impl"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(commonDep("junit:junit"))
    testApi(commonDep("com.android.tools:r8"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompileOnly(toolsJar())
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    /*
     * Actually those dependencies are needed only at runtime, but they
     *   declared as Api dependencies to propagate them to all modules
     *   which depend on current one
     */
    testApi(intellijDep()) {
        includeJars(
            "intellij-deps-fastutil-8.4.1-4",
            "idea_rt",
            "jps-model",
            "platform-impl",
            "streamex",
            "jna",
            rootProject = rootProject
        )
    }
    testImplementation(intellijDep()) {
        includeJars(
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            rootProject = rootProject
        )
    }
    testApiJUnit5()
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
