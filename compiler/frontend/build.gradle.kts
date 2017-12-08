
apply { plugin("kotlin") }

jvmTarget = "1.6"

val jflexPath by configurations.creating

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:util"))
    compile(project(":compiler:container"))
    compile(project(":compiler:resolution"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(commonDep("io.javaslang","javaslang"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("annotations", "trove4j", "guava-21.0") }
    jflexPath(commonDep("org.jetbrains.intellij.deps.jflex", "jflex"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

ant.importBuild("buildLexer.xml")

ant.properties["builddir"] = buildDir.absolutePath
ant.properties["flex.classpath"] = jflexPath.asPath
