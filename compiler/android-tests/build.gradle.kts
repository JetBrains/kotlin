
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(projectTests(":compiler:tests-common"))
    compile(commonDep("junit:junit"))
    compile(ideaSdkDeps("openapi"))

    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":core"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(ideaSdkDeps("jps-model.jar", subdir = "jps"))
    testCompile(ideaSdkDeps("groovy-all"))
    testCompile(ideaSdkDeps("idea", "idea_rt"))
    testCompile(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(ideaSdkDeps("jps-builders"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
