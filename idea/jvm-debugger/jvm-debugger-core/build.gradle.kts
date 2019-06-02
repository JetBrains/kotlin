plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:backend"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:light-classes"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:jvm-debugger:jvm-debugger-util"))
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("stream-debugger"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}