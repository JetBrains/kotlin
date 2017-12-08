
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("guava-21.0") }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-tooling-api-3.5") }
    compileOnly(intellijPluginDep("android")) { includeJars("android", "android-common", "sdk-common") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

