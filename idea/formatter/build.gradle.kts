
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkDeps("openapi"))
    compile(ideaSdkDeps("platform-api"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

