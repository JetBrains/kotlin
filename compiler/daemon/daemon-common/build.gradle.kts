
apply { plugin("kotlin") }

jvmTarget = "1.6"

repositories {
    mavenCentral()
    maven {
        setUrl("http://dl.bintray.com/kotlin/ktor")
    }
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-stdlib"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.22")
    compile("io.ktor:ktor-network:0.9.1-alpha-10")

}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

