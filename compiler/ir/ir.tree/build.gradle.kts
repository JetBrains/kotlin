
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

