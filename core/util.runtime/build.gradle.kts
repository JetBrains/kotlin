
apply {
    plugin("java")
    plugin("kotlin")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":core:builtins"))
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

