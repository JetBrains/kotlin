
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.serializer"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellij { include("annotations.jar", "guava-*.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

