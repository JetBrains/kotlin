plugins {
    kotlin("jvm")
    id("jps-compatible")
}

// Please make sure this module doesn't depend on `backend.js` (neither directly, nor transitively)
dependencies {
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":js:js.frontend"))
    compile(project(":compiler:cli"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:util"))

    testRuntime(project(":kotlin-reflect"))
    testRuntime(intellijDep()) { includeJars("picocontainer", "trove4j", "guava", "jdom", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}
