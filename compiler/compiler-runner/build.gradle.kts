
description = "Compiler runner + daemon client"

apply { plugin("kotlin") }

jvmTarget = "1.6"

val packIntoJar by configurations.creating

dependencies {
    val compile by configurations
    compile(project(":kotlin-build-common"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-preloader"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compile(project(":compiler:util"))
    packIntoJar(projectClasses(":compiler:daemon-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

evaluationDependsOn(":kotlin-daemon-client")

runtimeJar {
    from(packIntoJar)
    from(project(":kotlin-daemon-client").the<JavaPluginConvention>().sourceSets["main"].output.classesDirs)
}
sourcesJar()
javadocJar()

publish()
