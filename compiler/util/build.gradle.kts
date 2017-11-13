
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:deserialization"))
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

