
apply { plugin("kotlin") }

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

