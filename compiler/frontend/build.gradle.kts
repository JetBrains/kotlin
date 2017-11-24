
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

val jflexPath by configurations.creating

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:util"))
    compile(project(":compiler:container"))
    compile(project(":compiler:resolution"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(commonDep("io.javaslang","javaslang"))
    jflexPath(commonDep("org.jetbrains.intellij.deps.jflex", "jflex"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijCoreJar())
        compileOnly(intellij { include("annotations.jar", "trove4j.jar", "guava-*.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

ant.importBuild("buildLexer.xml")

ant.properties["builddir"] = buildDir.absolutePath
ant.properties["flex.classpath"] = jflexPath.asPath
