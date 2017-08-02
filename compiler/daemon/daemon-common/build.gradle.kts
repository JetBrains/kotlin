
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

