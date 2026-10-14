plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:cli-base"))
    testImplementation(project(":compiler:cli-jvm"))
    testImplementation(project(":core:compiler.common"))
    testImplementation(testFixtures(project(":compiler:tests-integration")))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

projectTests {
    testTask { }
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
    withJvmStdlibAndReflect()
}
