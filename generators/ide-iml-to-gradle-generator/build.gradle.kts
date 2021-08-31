plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val generateIdePluginGradleFiles by generator("org.jetbrains.kotlin.generators.imltogradle.MainKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
}
