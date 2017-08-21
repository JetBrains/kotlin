
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkDeps("openapi"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

