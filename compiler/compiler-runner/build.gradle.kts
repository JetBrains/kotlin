
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":build-common"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-preloader"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compile(project(":compiler:util"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

