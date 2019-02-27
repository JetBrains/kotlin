//SKIP-IF-RELEASE
pluginManagement {
    repositories {
        gradlePluginPortal()
#foreach($repo in $CIDR_CUSTOM_PLUGIN_REPOS)
        maven("$repo")
#end
    }
}
