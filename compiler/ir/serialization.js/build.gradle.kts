plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":js:js.frontend"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val generateIrRuntimeKlib by smartJavaExec {
    val inDirs = arrayOf(fileFrom(rootDir, "core", "builtins", "src"),
                         fileFrom(rootDir, "core", "builtins", "native"),
                         fileFrom(rootDir, "libraries", "stdlib", "common", "src"),
                         fileFrom(rootDir, "libraries", "stdlib", "src", "kotlin"),
                         fileFrom(rootDir, "libraries", "stdlib", "js", "src", "generated"),
                         fileFrom(rootDir, "libraries", "stdlib", "js", "irRuntime"),
                         fileFrom(rootDir, "libraries", "stdlib", "unsigned"),
                         fileFrom(rootDir, "js", "js.translator", "testData", "_commonFiles"))
    inDirs.forEach { inputs.dir(it) }

    val outDir = "$rootDir/js/js.translator/testData/out/klibs/"
    outputs.dir(outDir)

    classpath = sourceSets.test.get().runtimeClasspath
    main = "org.jetbrains.kotlin.ir.backend.js.GenerateIrRuntimeKt"
    workingDir = rootDir
}

testsJar {}