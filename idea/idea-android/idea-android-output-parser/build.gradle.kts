
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(ideaPluginDeps("gradle-api", plugin = "gradle"))
    compile(ideaPluginDeps("android", "android-common", "android-base-common", "sdk-common", plugin = "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

