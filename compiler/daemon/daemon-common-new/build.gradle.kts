plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

val ktorExcludesForDaemon: List<Pair<String, String>> by rootProject.extra

dependencies {
    compileOnly(project(":daemon-common"))
    api(kotlinStdlib())
    compileOnly(intellijCore())
    api(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    api(commonDependency("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
    api(commonDependency("io.ktor", "ktor-utils")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks {
    val compileKotlin by existing(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
        }
    }
}
