
apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
    setPlugins("gradle", "android")
}

dependencies {
    compile(project(":compiler:util"))
}

afterEvaluate {
    dependencies {
        compile(intellijCoreJar())
        compile(intellijPlugin("gradle") { include("gradle-tooling-api.jar") })
        compile(intellijPlugin("android") { include("android.jar", "android-common.jar", "sdk-common.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

