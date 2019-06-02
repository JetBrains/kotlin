plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:fir:tree"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }

    testCompileOnly(intellijDep()) { includeJars("openapi", "java-api", "idea", "idea_rt", "util", "asm-all", "extensions", rootProject = rootProject) }

    testRuntime(intellijDep())

    testCompile(commonDep("junit:junit"))
    testCompileOnly(project(":kotlin-test:kotlin-test-jvm"))
    testCompileOnly(project(":kotlin-test:kotlin-test-junit"))
    testCompile(projectTests(":compiler:tests-common"))
    
    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"
}

testsJar()
