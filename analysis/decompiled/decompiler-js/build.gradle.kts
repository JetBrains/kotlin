plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
}

dependencies {
    api(kotlinStdlib())
    implementation(project(":analysis:decompiled:decompiler-to-psi"))

    compileOnly(intellijCore())
}
