
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
    compile(ideaSdkCoreDeps("intellij-core", "util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

