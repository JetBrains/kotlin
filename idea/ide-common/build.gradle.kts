
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.serializer"))
    compile(ideaSdkCoreDeps("annotations", "guava", "intellij-core"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

