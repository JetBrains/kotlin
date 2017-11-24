
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin()

dependencies {
    compile(project(":compiler:util"))
    compile(project(":core:descriptors"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("trove4j.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
