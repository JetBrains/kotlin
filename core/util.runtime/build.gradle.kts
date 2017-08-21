
apply {
    plugin("java")
    plugin("kotlin")
}

dependencies {
    compile(project(":core:builtins"))
    compile(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

