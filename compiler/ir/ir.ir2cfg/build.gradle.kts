
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

