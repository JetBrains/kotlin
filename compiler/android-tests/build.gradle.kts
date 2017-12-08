
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(projectTests(":compiler:tests-common"))
    compile(commonDep("junit:junit"))
    compileOnly(intellijDep()) { includeJars("openapi") }

    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
    testCompile(intellijDep()) { includeJars("openapi", "idea", "idea_rt", "groovy-all-2.4.6", "jps-builders") }
    testCompile(intellijDep("jps-standalone")) { includeJars("jps-model") }
    testCompile(intellijDep("jps-build-test"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
