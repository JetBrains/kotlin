pluginManagement {
    val rootProperties = java.util.Properties().apply {
        rootDir.resolve("../gradle.properties").reader().use(::load)
    }

    val kotlinCompilerRepo: String by rootProperties
    val kotlinVersion by rootProperties

    repositories {
        maven(kotlinCompilerRepo)
        maven("https://cache-redirector.jetbrains.com/maven-central")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
            }
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
            }
        }
    }
}

rootProject.name = "kotlin-native-build-tools"

includeBuild("../shared")
