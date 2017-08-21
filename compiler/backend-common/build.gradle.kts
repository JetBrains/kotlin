
apply { plugin("kotlin") }

dependencies {
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:cli-common"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../ir/backend.common/src")
    }
    "test" {}
}
