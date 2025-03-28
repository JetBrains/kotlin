plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("17")

dependencies {
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-deserialization"))
    implementation("org.eclipse.jdt:ecj:3.41.0")

    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<Test> {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}
