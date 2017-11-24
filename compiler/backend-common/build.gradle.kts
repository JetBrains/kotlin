
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijCoreJar())
    }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../ir/backend.common/src")
    }
    "test" {}
}
