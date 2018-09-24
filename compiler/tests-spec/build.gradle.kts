plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(projectTests(":compiler"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerSpecTestsKt")

val printSpecTestsStatistic by smartJavaExec {
    classpath = javaPluginConvention().sourceSets.getByName("test").runtimeClasspath
    main = "org.jetbrains.kotlin.spec.tasks.PrintSpecTestsStatisticKt"
}

val generateJsonTestsMap by smartJavaExec {
    classpath = javaPluginConvention().sourceSets.getByName("test").runtimeClasspath
    main = "org.jetbrains.kotlin.spec.tasks.GenerateJsonTestsMapKt"
}
