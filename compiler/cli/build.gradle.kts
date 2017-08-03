
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:serialization"))
    compile(project(":compiler:plugin-api"))
    compile(project(":js:js.translator"))
    compile(project(":js:js.serializer"))
    compile(project(":js:js.dce"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
}

configureKotlinProjectSources("compiler/cli/src",
                              "plugins/annotation-collector/src",
                              "compiler/builtins-serializer/src",
                              "compiler/javac-wrapper/src",
                              sourcesBaseDir = rootDir)
configureKotlinProjectResourcesDefault()
configureKotlinProjectNoTests()

