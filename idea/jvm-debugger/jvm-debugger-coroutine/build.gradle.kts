plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":idea:jvm-debugger:jvm-debugger-core"))

    compileOnly(toolsJarApi())
    compileOnly(intellijDep())

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}