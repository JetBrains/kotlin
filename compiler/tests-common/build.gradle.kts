
apply { plugin("kotlin") }

dependencies {
    compile(project(":core"))
    compile(project(":core::util.runtime"))
    compile(project(":compiler:util"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:serialization"))
    compile(project(":kotlin-preloader"))
    compile(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compile(project(":js:js.serializer"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.translator"))
    compileOnly(project(":plugins:android-extensions-compiler"))
    compile(project(":kotlin-test:kotlin-test-jvm"))
    compile(project(":compiler:tests-common-jvm6"))
    compile(commonDep("junit:junit"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(ideaSdkDeps("openapi", "idea", "idea_rt"))
    compile(preloadedDeps("dx", subdir = "android-5.0/lib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
