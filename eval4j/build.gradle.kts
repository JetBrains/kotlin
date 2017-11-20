
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(projectDist(":kotlin-reflect"))
    compile(project(":compiler:backend"))
    compile(ideaSdkDeps("asm-all"))
//    compile(files(PathUtil.getJdkClassesRootsFromCurrentJre())) // TODO: make this one work instead of the nex one, since it contains more universal logic
    compile(files("${System.getProperty("java.home")}/../lib/tools.jar"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
}
