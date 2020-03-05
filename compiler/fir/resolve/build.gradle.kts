plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":core:deserialization"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:fir:tree"))
    compile(project(":compiler:frontend"))
    compile("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.2")

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }

    Platform[193].orLower {
        testCompileOnly(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "util", "asm-all", "extensions", rootProject = rootProject) }
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }

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
