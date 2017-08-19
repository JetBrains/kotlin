
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(ideaPluginDeps("gradle-tooling-api", plugin = "gradle"))
    compile(ideaPluginDeps("android", "android-common", "sdk-common", plugin = "android"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

