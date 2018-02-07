
apply { plugin("kotlin") }

jvmTarget = "1.8"

dependencies {
    compile(project(":compiler:util"))
//    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend"))
    compile(project(":js:js.translator"))
//    compile(project(":compiler:backend-common"))
//    compile(project(":js:js.serializer"))
//    compile(project(":js:js.dce"))
//    compile(commonDep("org.fusesource.jansi", "jansi"))
//    compile(commonDep("org.jline", "jline"))
//    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

//    testCompile(project(":compiler:backend"))
//    testCompile(project(":compiler:cli"))
//    testCompile(project(":compiler:tests-common"))
//    testCompile(projectTests(":compiler:tests-common"))
//    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

//testsJar {}

//projectTest {
//    workingDir = rootDir
//}