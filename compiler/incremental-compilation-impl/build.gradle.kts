
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:cli"))
    compile(project(":kotlin-build-common"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":kotlin-stdlib"))
    testCompile(projectTests(":kotlin-build-common"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectTestsDefault()

testsJar()
