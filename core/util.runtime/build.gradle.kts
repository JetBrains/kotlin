
apply {
    plugin("java")
    plugin("kotlin")
}

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

