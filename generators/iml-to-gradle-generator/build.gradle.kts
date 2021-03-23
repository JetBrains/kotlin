plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(kotlinStdlib())
    testCompile(intellijDep())
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

val generateGradleByIml by generator("org.jetbrains.kotlin.generators.imltogradle.MainKt") {
    args = args.orEmpty() + if (project.hasProperty("args")) (project.property("args") as String).split(" ") else listOf()
}
