
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":core:deserialization"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:fir:tree"))
    testCompile(project(":compiler:fir:psi2fir"))
    testCompile(project(":compiler:fir:fir2ir"))
    testCompile(project(":compiler:fir:cones"))
    testCompile(project(":compiler:fir:resolve"))
    testCompile(project(":compiler:fir:java"))
    testCompile(project(":compiler:ir.ir2cfg"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-js"))
    testCompile(project(":compiler:light-classes"))
    testCompile(project(":compiler:serialization"))
    testCompile(project(":kotlin-preloader"))
    testCompile(project(":compiler:daemon-common"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.frontend"))
    testCompile(project(":js:js.translator"))
    testCompileOnly(project(":plugins:android-extensions-compiler"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":compiler:tests-common-jvm6"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":kotlin-scripting-compiler-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(androidDxJar()) { isTransitive = false }
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(intellijDep()) {
        includeJars("openapi", "platform-api", "platform-impl", "idea", "idea_rt", "guava", "trove4j", "picocontainer", "asm-all", "log4j", "jdom", "bootstrap", rootProject = rootProject)
        isTransitive = false
    }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
