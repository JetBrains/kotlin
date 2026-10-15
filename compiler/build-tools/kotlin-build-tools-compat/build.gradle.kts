plugins {
    id("common-configuration")
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-build-tools-api:2.5.0-Beta1") // KT-90734 [BTA] Update compat BTA API version to 2.5.0 stable
    compileOnly(libs.kotlin.compiler.before.bta.v2)
    implementation(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`

    testCompileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    testImplementation(kotlinTest("junit5"))
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler {
    from(mainSourceSet.output)
})
sourcesJar()
javadocJar()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}
