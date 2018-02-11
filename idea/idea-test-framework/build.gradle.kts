
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.script"))
    compile(projectTests(":compiler:tests-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":kotlin-test:kotlin-test-jvm"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(commonDep("junit:junit"))
    compileOnly(intellijDep()) { includeJars("openapi", "idea", "log4j") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


