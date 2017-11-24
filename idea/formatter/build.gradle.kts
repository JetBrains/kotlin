
apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("idea.jar", "openapi.jar", "util.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

