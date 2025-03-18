plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

sourceSets {
    "main" {
        projectDefault()
    }

    "test" {
        projectDefault()
    }
}

dependencies {
    api(kotlinStdlib())

    implementation(libs.kotlinx.serialization.json)

    testApi(kotlinTest("junit5"))
    testCompileOnly(libs.junit.jupiter.api)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
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
