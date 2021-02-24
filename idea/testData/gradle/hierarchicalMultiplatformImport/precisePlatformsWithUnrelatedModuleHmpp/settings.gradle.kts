pluginManagement {
    repositories {
        {{kts_kotlin_plugin_repositories}}
    }
}

rootProject.name = "my-app"

enableFeaturePreview("GRADLE_METADATA")

include(":submodule")