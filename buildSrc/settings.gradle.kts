import java.lang.Boolean.parseBoolean

val cacheRedirectorEnabled: String? by settings

pluginManagement {
    repositories {
        if (parseBoolean(cacheRedirectorEnabled)) {
            maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        }
        gradlePluginPortal()
    }
}

include(":prepare-deps:platform-deps",
        ":prepare-deps:cidr",
        ":prepare-deps:idea-plugin"
)
