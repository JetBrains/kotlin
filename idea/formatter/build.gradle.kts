
apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
}

afterEvaluate {
    dependencies {
        compile(intellij { include("openapi.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

