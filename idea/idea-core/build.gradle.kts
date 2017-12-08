apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:util"))
    compile(project(":j2k"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:android-extensions-compiler"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("util", "openapi", "idea", "asm-all", "jdom", "annotations", "trove4j", "guava-21.0") }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-tooling-api-3.5", "gradle") }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../idea-analysis/src")
        resources.srcDir("../idea-analysis/src").apply { include("**/*.properties") }
    }
    "test" {}
}
