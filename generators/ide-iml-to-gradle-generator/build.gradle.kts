import org.gradle.kotlin.dsl.invoke

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    implementation(jpsModel()) {
        exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
    }
    implementation("com.jetbrains.intellij.platform:util-text-matching:$intellijVersion")
    implementation(intellijPlatformUtil()) {
        exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
    }
    implementation(jpsModelImpl()) {
        exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
    }
    implementation(jpsModelSerialization()) {
        exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
    }
    implementation(commonDependency("com.google.code.gson:gson"))
    implementation(intellijJDom())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val generateIdePluginGradleFiles by generator("org.jetbrains.kotlin.generators.imltogradle.MainKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
}
