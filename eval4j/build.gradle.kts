
apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:backend"))
//    compile(files(PathUtil.getJdkClassesRootsFromCurrentJre())) // TODO: make this one work instead of the nex one, since it contains more universal logic
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("asm-all.jar") })
        testCompile(intellij { include("asm-all.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
}
