
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

