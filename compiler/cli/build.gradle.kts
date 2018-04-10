
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:serialization"))
    compile(project(":compiler:plugin-api"))
    compile(project(":js:js.translator"))
    compile(project(":js:js.serializer"))
    compile(project(":js:js.dce"))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("org.jline", "jline"))
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../../plugins/annotation-collector/src",
                     "../builtins-serializer/src",
                     "../javac-wrapper/src")
    }
    "test" {
        java.srcDirs("../../plugins/annotation-collector/test")
    }
}

testsJar {}

projectTest {
    workingDir = rootDir
}