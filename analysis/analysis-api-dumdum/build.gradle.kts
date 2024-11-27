plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

dependencies {
    implementation(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    implementation(commonDependency("org.codehaus.woodstox:stax2-api"))
    implementation(commonDependency("com.fasterxml:aalto-xml"))
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-fir"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))

}



sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

application {    
    mainClass.set("org.jetbrains.kotlin.analysis.api.dumdum.ScratchKt")
    
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}