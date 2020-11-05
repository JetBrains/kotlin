plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":idea:jvm-debugger:jvm-debugger-core"))
    implementation("org.apache.maven:maven-artifact:3.6.3")

    compileOnly(toolsJarApi())
    compileOnly(intellijDep())

    compileOnly(intellijPluginDep("java"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}