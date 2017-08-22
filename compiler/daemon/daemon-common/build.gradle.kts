
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

