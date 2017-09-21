
description = "Compiler runner + daemon client"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-build-common"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-preloader"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compileOnly(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar {
    from(getSourceSetsFrom(":kotlin-daemon-client")["main"].output.classesDirs)
    from(getSourceSetsFrom(":compiler:daemon-common")["main"].output.classesDirs)
}
sourcesJar()
javadocJar()

publish()
