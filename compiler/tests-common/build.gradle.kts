
apply { plugin("kotlin") }

dependencies {
    testCompile(project(":core"))
    testCompile(project(":core::util.runtime"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:light-classes"))
    testCompile(project(":compiler:serialization"))
    testCompile(project(":kotlin-preloader"))
    testCompile(project(":compiler:daemon-common"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.frontend"))
    testCompile(project(":js:js.translator"))
    testCompileOnly(project(":plugins:android-extensions-compiler"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":compiler:tests-common-jvm6"))
    testCompile(commonDep("junit:junit"))
    testCompile(ideaSdkCoreDeps("intellij-core"))
    testCompile(ideaSdkDeps("openapi", "idea", "idea_rt"))
    testCompile(preloadedDeps("dx", subdir = "android-5.0/lib"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
