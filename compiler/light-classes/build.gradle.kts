
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

