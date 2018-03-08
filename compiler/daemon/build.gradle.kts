
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:cli"))
    compile(project(":compiler:daemon-common"))
    compile(project(":compiler:incremental-compilation-impl"))
    compile(project(":kotlin-build-common"))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
