import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:cli"))
    compile(project(":compiler:daemon-common"))
    compile(project(":compiler:incremental-compilation-impl"))
    compile(project(":kotlin-build-common"))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    compile(projectDist(":kotlin-reflect"))
    compile(project(":kotlin-reflect-api"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compile("io.ktor:ktor-network:0.9.1-alpha-10") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
