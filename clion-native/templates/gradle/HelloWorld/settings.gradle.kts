//SKIP-IF-RELEASE
pluginManagement {
#if($CIDR_PLUGIN_RESOLUTION_RULES)$CIDR_PLUGIN_RESOLUTION_RULES#end
    repositories {
        gradlePluginPortal()
#foreach($repo in $CIDR_CUSTOM_PLUGIN_REPOS)
        maven("$repo")
#end
    }
}
