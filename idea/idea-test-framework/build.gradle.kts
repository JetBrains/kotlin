
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.script"))
    compile(projectTests(":compiler:tests-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":kotlin-test:kotlin-test-jvm"))
    compile(commonDep("junit:junit"))
    compile(ideaSdkDeps("openapi", "idea"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


