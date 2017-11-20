apply { plugin("kotlin") }

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

dependencies {
    compile(project(":core:util.runtime"))
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}
