
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

