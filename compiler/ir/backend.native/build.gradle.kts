plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":native:kotlin-native-utils"))
    api(project(":native:base"))

    testApi(commonDependency("commons-lang:commons-lang"))
    testApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testApi(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testApi(project(":kotlin-util-klib-abi"))
    testApi(projectTests(":kotlin-util-klib-abi"))
    testApi(project(":kotlin-compiler-runner-unshaded"))
    testApi(project(":native:executors"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(project(":compiler:cli-common"))
    testImplementation(project(":compiler:ir.serialization.native"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

}

optInToUnsafeDuringIrConstructionAPI()

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar {}
