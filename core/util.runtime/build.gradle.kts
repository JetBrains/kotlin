
apply {
    plugin("java")
    plugin("kotlin")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":core:builtins"))
    compile(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

