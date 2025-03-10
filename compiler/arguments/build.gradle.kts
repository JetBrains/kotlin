plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../builtins-serializer/src")
    }
}
