import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:semantics"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.serialization.common"))
    compileOnly(project(":compiler:fir:fir-serialization"))
    compileOnly(project(":compiler:fir:fir-deserialization"))

    compileOnly(intellijCore())

    testCompileOnly(kotlinTest("junit"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:fir:analysis-tests"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(project(":core:deserialization"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":core:descriptors.jvm"))
    testRuntimeOnly(project(":compiler:fir:fir2ir:jvm-backend"))
    testRuntimeOnly(project(":generators"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    testRuntimeOnly(commonDependency("one.util:streamex"))

    testRuntimeOnly(jpsModel())
    testRuntimeOnly(jpsModelImpl())
}

optInToObsoleteDescriptorBasedAPI()

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
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

fun Test.configure(configureJUnit: JUnitPlatformOptions.() -> Unit = {}) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform {
        configureJUnit()
    }
}

projectTest(
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)
) {
    configure()
}

projectTest("aggregateTests", jUnitMode = JUnitMode.JUnit5) {
    configure {
        excludeTags("FirPsiCodegenTest")
    }
}

projectTest("nightlyTests", jUnitMode = JUnitMode.JUnit5) {
    configure {
        includeTags("FirPsiCodegenTest")
    }
}

testsJar()
