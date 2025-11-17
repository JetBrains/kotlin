plugins {
    kotlin("jvm")
    application
    id("project-tests-convention")
}

dependencies {
    implementation(project(":compiler:cli:cli-arguments-generator"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:arguments"))
    implementation(project(":compiler:build-tools:kotlin-build-tools-api-custom-types"))
    implementation(project(":compiler:build-tools:util-kotlinpoet"))
    implementation(project(":generators"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
}

application {
    mainClass.set("org.jetbrains.kotlin.buildtools.options.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        projectDefault()
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJUnitPlatform()
    }
}
