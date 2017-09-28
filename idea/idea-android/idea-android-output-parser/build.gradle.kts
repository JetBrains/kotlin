
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(ideaPluginDeps("gradle-tooling-api", plugin = "gradle"))
    compile(ideaPluginDeps("android", "android-common", "sdk-common", /*"common",*/ plugin = "android"))
    compile(ideaSdkDeps("android-base-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

