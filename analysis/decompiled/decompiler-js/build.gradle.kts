plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
}

dependencies {
    implementation(project(":core:deserialization"))
    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))

    implementation(project(":js:js.serializer"))

    compileOnly(intellijCore())
}
