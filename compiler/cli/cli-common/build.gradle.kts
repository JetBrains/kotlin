
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

