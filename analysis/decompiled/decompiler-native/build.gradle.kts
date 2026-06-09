plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

sourceSets {
    "main" { projectDefault() }
}

dependencies {
    implementation(project(":core:deserialization"))
    api(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:frontend.java"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":kotlin-util-klib-metadata"))

    compileOnly(intellijCore())

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit4)
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
}

optInToK1Deprecation()
