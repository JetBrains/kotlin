
apply {
    plugin("java")
    plugin("kotlin")
}

dependencies {
    val compile by configurations
    compile(project(":core:builtins"))
    compile(kotlinDep("stdlib"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

