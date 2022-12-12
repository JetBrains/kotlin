plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    implementation(kotlinBuiltins())
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:serialization"))
    api(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

publish()

standardPublicJars()

val dumpKlib by task<JavaExec> {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.jetbrains.kotlin.library.klibdump.KlibDump")
}

