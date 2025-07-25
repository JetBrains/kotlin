import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("compiler-tests-convention")
}

description = "Contains a unified representation of Kotlin compiler arguments for current and old Kotlin releases."

sourceSets {
    "main" {
        projectDefault()
    }

    "test" {
        projectDefault()
    }
}

publish {
    artifactId = "kotlin-compiler-arguments-description"
}
standardPublicJars()

// schema-kenerator-* dependency is only compatible with JDK 11+
tasks.named<KotlinJvmCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

dependencies {
    api(kotlinStdlib())

    implementation(libs.kotlinx.serialization.json)

    compileOnly(project(":compiler:arguments.common"))
    embedded(project(":compiler:arguments.common")) {
        isTransitive = false
    }

    testApi(kotlinTest("junit5"))
    testImplementation(project(":compiler:config.jvm"))

    testCompileOnly(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(project(":compiler:util"))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(libs.schema.kenerator.core)
    testImplementation(libs.schema.kenerator.serialization)
    testImplementation(libs.schema.kenerator.jsonschema)
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        javaLauncher.value(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}

val generateJson = tasks.register<JavaExec>("generateJson") {
    classpath(sourceSets.named("main").flatMap { it.kotlin.classesDirectory })
    classpath(sourceSets.named("main").map { it.compileClasspath })
    mainClass.set("org.jetbrains.kotlin.arguments.serialization.json.JsonSerializerKt")

    val outputJsonInResources = sourceSets.named("main").map {
        // The first one is the default one
        it.resources.srcDirs.last().resolve("kotlin-compiler-arguments.json")
    }
    outputs.file(outputJsonInResources)
    argumentProviders.add {
        listOf(outputJsonInResources.get().path)
    }

}

tasks.named("processResources") {
    dependsOn(generateJson)
}
tasks.named("sourcesJar") {
    dependsOn(generateJson)
}
