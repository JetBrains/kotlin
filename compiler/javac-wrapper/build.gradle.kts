plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:util"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { }
}

optInToK1Deprecation()
