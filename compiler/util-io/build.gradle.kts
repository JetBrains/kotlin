plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    testImplementation(commonDependency("junit:junit"))
    testImplementation(kotlin("test"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.4"
            apiVersion = "1.4"
            freeCompilerArgs += listOf("-Xsuppress-version-warnings", "-Xinline-classes")
        }
    }
}

publish()

standardPublicJars()
