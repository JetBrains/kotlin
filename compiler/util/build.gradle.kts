
apply { plugin("kotlin") }

dependencies {
    compile(project(":core"))
    compile(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    compile(ideaSdkDeps("jps-model.jar", subdir = "jps"))
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(File(rootDir, "resources")).apply { include("**") }
    }
    "test" {}
}

