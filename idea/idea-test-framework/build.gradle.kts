
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler.tests-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-jps-common"))
    compile(ideaSdkDeps("openapi", "idea"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()


