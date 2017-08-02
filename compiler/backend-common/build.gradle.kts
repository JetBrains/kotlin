
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
}

configureKotlinProjectSources("backend-common/src", "ir/backend.common/src", sourcesBaseDir = File(rootDir, "compiler"))
configureKotlinProjectNoTests()

