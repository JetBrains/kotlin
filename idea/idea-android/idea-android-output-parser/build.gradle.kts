
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
        compileOnly(intellijCoreJar())
        compileOnly(intellij { include("guava-*.jar") })
        compileOnly(intellijPlugin("gradle") { include("gradle-tooling-api.jar") })
        compileOnly(intellijPlugin("android") { include("android.jar", "android-common.jar", "sdk-common.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

