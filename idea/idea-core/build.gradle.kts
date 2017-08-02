apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-stdlib"))
    compile(project(":core"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compile(project(":j2k"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:android-extensions-compiler"))
    compile(ideaSdkCoreDeps("intellij-core", "util"))
    compile(ideaSdkDeps("openapi", "idea"))
    compile(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
    compile(preloadedDeps("uast-common", "kotlinx-coroutines-core", "kotlinx-coroutines-jdk8"))
    buildVersion()
}

configureKotlinProjectSources("idea-core/src", "idea-analysis/src", sourcesBaseDir = File(rootDir, "idea"))
configureKotlinProjectResources("idea-analysis/src", sourcesBaseDir = File(rootDir, "idea")) {
    include("**/*.properties")
}
configureKotlinProjectNoTests()
