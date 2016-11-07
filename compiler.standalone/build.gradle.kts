
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

dependencies {
    compile(project(":compiler"))
    compile(fileTree(mapOf("dir" to "$rootDir/ideaSDK/core", "include" to "*.jar")))
    compile(commonDep("org.fusesource.jansi", "jansi"))
    compile(commonDep("jline"))
}

configureKotlinProjectSources(
        "compiler/cli/src",
        "compiler/daemon/src",
        "compiler/builtins-serializer/src",
        "compiler/conditional-preprocessor/src",
        "plugins/annotation-collector/src",
        sourcesBaseDir = rootDir)
configureKotlinProjectNoTests()

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-compiler.standalone")
}

fixKotlinTaskDependencies()
