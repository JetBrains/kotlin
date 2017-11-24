
apply { plugin("kotlin") }

configureIntellijPlugin()

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
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "idea.jar", "log4j.jar") })
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


