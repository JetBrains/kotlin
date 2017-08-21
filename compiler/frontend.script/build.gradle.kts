
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":kotlin-reflect"))
    compile(preloadedDeps("kotlinx-coroutines-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

