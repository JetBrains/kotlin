
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("jdom", "util") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

