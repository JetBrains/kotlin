import com.sun.javafx.scene.CameraHelper.project

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-stdlib"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    compile(projectDist(":kotlin-reflect"))
    compile(project(":kotlin-reflect-api"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compile("io.ktor:ktor-network:0.9.1-alpha-10") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
