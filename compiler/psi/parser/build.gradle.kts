plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val jflexPath = configurations.create("jflexPath")

dependencies {
    compileOnly(intellijCore())
    compileOnly(libs.org.jetbrains.annotations)

    implementation(project(":compiler:psi:psi-api"))

    jflexPath(commonDependency("org.jetbrains.intellij.deps.jflex", "jflex")) {
        // Flex brings many unrelated dependencies, so we are dropping them because only a flex `.jar` file is needed.
        // It can be probably removed when https://github.com/JetBrains/intellij-deps-jflex/issues/10 is fixed.
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

@Suppress("DEPRECATION") // Project.ant is deprecated in Gradle 9.9, removed in Gradle 10: https://docs.gradle.org/9.9.2/userguide/upgrading_version_9.html
ant.importBuild("buildLexer.xml")

@Suppress("DEPRECATION")
ant.properties["builddir"] = layout.buildDirectory.get().asFile.absolutePath

tasks.findByName("lexer")!!.apply {
    doFirst {
        @Suppress("DEPRECATION")
        ant.properties["flex.classpath"] = jflexPath.asPath
    }
}
