plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:backend"))
    compile(project(":idea:ide-common"))
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