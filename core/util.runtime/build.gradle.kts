
apply {
    plugin("java")
    plugin("kotlin")
}

dependencies {
    val compile by configurations
    compile(project(":core:builtins"))
    compile(project(":kotlin-stdlib"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

