plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:fir:tree"))
    compile(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:fir2ir"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
