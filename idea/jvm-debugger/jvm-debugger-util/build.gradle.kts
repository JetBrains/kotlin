plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:backend"))
    compile(project(":idea:idea-core"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))

    // TODO: get rid of this
    compile(project(":idea:jvm-debugger:eval4j"))

    compileOnly(toolsJarApi())

    compileOnly(intellijPluginDep("java"))

    compileOnly(intellijDep())

    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

jvmTarget = "11"
javaHome = rootProject.extra["JDK_11"] as String