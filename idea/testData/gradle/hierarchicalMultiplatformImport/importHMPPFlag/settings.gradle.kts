pluginManagement {
    repositories {
        {{kts_kotlin_plugin_repositories}}
    }
    {{kts_resolution_strategy}}
}

rootProject.name = "my-app"

enableFeaturePreview("GRADLE_METADATA")