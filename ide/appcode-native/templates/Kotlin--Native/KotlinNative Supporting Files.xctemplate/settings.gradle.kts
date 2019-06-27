pluginManagement {@@MPP_PLUGIN_RESOLUTION_RULES@@
    repositories {
        gradlePluginPortal()@@MPP_CUSTOM_PLUGIN_REPOS_8S@@
    }
}

include(":___PARENTPACKAGENAME___")
project(":___PARENTPACKAGENAME___").projectDir = file("../___PARENTPACKAGENAME___")
rootProject.name = file("..").name
