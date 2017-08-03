
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:cli-common"))
}

configureKotlinProjectSources("backend-common/src", "ir/backend.common/src", sourcesBaseDir = File(rootDir, "compiler"))
configureKotlinProjectNoTests()

