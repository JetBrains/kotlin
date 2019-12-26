plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(projectTests(":compiler"))
    Platform[192].orHigher {
        testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
        testRuntimeOnly(intellijPluginDep("java"))
    }
    compile("org.jsoup:jsoup:1.10.3")
    if (System.getProperty("idea.active") != null) testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}

val generateSpecTests by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateSpecTestsKt")

val generateFeatureInteractionSpecTestData by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateFeatureInteractionSpecTestDataKt")

val printSpecTestsStatistic by generator("org.jetbrains.kotlin.spec.utils.tasks.PrintSpecTestsStatisticKt")

val generateJsonTestsMap by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateJsonTestsMapKt")

val remoteRunTests by task<Test> {
    val packagePrefix = "org.jetbrains.kotlin.spec."
    val includeTests = setOf(
        "checkers.DiagnosticsTestSpecGenerated\$NotLinked\$Contracts*",
        "checkers.DiagnosticsTestSpecGenerated\$NotLinked\$Annotations*",
        "checkers.DiagnosticsTestSpecGenerated\$NotLinked\$Local_variables\$Type_parameters*",
        "checkers.DiagnosticsTestSpecGenerated\$NotLinked\$Dfa*",
        "codegen.BlackBoxCodegenTestSpecGenerated\$NotLinked\$Annotations\$Type_annotations*",
        "codegen.BlackBoxCodegenTestSpecGenerated\$NotLinked\$Objects\$Inheritance*"
    )

    workingDir = rootDir

    filter {
        includeTests.forEach { includeTestsMatching(packagePrefix + it) }
    }
}

val specConsistencyTests by task<Test> {
    workingDir = rootDir

    filter {
        includeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
}
