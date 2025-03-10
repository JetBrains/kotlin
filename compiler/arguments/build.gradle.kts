plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    "main" {
        projectDefault()
    }
}

dependencies {
    api(kotlinStdlib())
}
