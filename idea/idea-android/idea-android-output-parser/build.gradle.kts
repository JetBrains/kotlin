
apply { plugin("kotlin") }

apply { plugin("jps-compatible") }

dependencies {
    compile(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("guava", "android-base-common", rootProject = rootProject) }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-api", rootProject = rootProject) }
    compileOnly(intellijPluginDep("android")) { includeJars("android", "android-common", "android-base-common", "sdk-common-26.0.0") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

