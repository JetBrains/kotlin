
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(kotlinStdlib("jdk8"))
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":core:deserialization"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:tests-mutes"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:ir.ir2cfg"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:psi"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-js"))
    testCompile(project(":compiler:serialization"))
    testCompile(project(":compiler:fir:entrypoint"))
    testCompile(project(":compiler:backend.jvm:backend.jvm.entrypoint"))
    testCompile(projectTests(":compiler:test-infrastructure-utils"))
    testCompile(project(":kotlin-preloader"))
    testCompile(commonDep("com.android.tools:r8"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(intellijDep()) {
        includeJars(
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "idea_rt",
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            rootProject = rootProject
        )
        isTransitive = false
    }
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar {}
