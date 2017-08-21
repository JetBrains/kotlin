
apply { plugin("kotlin") }

dependencies {
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:container"))
    compile(project(":compiler:resolution"))
    compile(project(":kotlin-script-runtime"))
    compile(commonDep("io.javaslang","javaslang"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

