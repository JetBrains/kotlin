plugins {
    kotlin("jvm")
    id("jps-compatible")
}

kotlin {
    explicitApiWarning()
}

dependencies {
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":analysis:low-level-api-fir"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:analysis-api-providers"))

    api(intellijCore())
    api(commonDependency("com.google.guava:guava"))
}

kotlin {
    explicitApi()
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
