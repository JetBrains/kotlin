plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    "main" {
        projectDefault()
    }

    "test" {
        projectDefault()
    }
}

dependencies {
    api(kotlinStdlib())

    testApi(kotlinTest("junit5"))
    testCompileOnly(libs.junit.jupiter.api)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
}