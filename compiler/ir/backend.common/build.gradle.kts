
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    val compile by configurations
    val compileOnly by configurations

    compile(project(":core:descriptors"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
