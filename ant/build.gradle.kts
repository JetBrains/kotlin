
description = "Kotlin Ant Tools"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(commonDep("org.apache.ant", "ant"))
    compile(project(":kotlin-preloader"))
    compile(project(":kotlin-stdlib"))
    buildVersion()
}

sourceSets {
    "main" { default() }
    "test" { none() }
}

runtimeJar {
    from("$projectDir/src") { include("**/*.xml") }
    manifest.attributes.put("Class-Path", "kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar kotlin-preloader.jar")
}

dist()

