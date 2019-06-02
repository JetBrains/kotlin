plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:backend"))
    compile(project(":idea:jvm-debugger:eval4j"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-j2k"))
    compile(project(":idea:jvm-debugger:jvm-debugger-util"))
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))

    compileOnly(intellijDep())

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}