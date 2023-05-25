import org.jetbrains.kotlin.ideaExt.idea

/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCore())

    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testApi(project(":compiler:fir:checkers:checkers.js"))
    testApi(project(":compiler:fir:checkers:checkers.native"))
    testApi(project(":compiler:fir:fir-serialization"))
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:frontend"))

    testApiJUnit5()

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir2ir:jvm-backend"))

    testImplementation(intellijCore())

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    testRuntimeOnly(commonDependency("one.util:streamex"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    testRuntimeOnly(toolsJar())
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

fun Test.configureTest(configureJUnit: JUnitPlatformOptions.() -> Unit = {}) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform {
        configureJUnit()
    }
    useJsIrBoxTests(version = version, buildDir = "$buildDir/")
}


projectTest(
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)
) {
    configureTest {
        excludeTags("Jdk21Test")
    }
}

// Separate configuration is only necessary while JDK 21 is not released, so cannot be obtained via toolchain.
// See KT-58765 for tracking
projectTest(
    "jdk21Tests",
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(
        JdkMajorVersion.JDK_21_0
    )
) {
    configureTest {
        includeTags("Jdk21Test")
    }
}

testsJar()
