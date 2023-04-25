plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":compiler:container"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
