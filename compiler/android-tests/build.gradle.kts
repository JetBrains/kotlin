
apply { plugin("kotlin") }

jvmTarget = "1.6"

configureIntellijPlugin {
    setExtraDependencies("jps-standalone", "jps-build-test")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(projectTests(":compiler:tests-common"))
    compile(commonDep("junit:junit"))

    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(project(":core:descriptors"))
    testCompile(project(":core:descriptors.jvm"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
}

afterEvaluate {
    dependencies {
        compile(intellij { include("openapi.jar") })
        testCompile(intellij { include("idea.jar", "idea-rt.jar", "groovy-all.jar", "jps-builders.jar") })
        testCompile(intellijExtra("jps-standalone") { include("jps-model.jar") })
        testCompile(intellijExtra("jps-build-test"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
