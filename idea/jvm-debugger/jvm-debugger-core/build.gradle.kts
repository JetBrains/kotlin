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

    compileOnly(toolsJarApi())
    compileOnly(intellijDep())
    
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }

    compileOnly(intellijPluginDep("stream-debugger"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}