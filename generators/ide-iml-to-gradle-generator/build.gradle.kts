plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(jpsModel())
    implementation("com.jetbrains.intellij.platform:util-text-matching:$intellijVersion")
    implementation(intellijPlatformUtil())
    implementation(jpsModelImpl())
    implementation(jpsModelSerialization())
    implementation(commonDependency("com.google.code.gson:gson"))
    implementation(intellijJDom())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val generateIdePluginGradleFiles by generator("org.jetbrains.kotlin.generators.imltogradle.MainKt", mainSourceSet) {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
}
