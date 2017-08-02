
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":core"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

