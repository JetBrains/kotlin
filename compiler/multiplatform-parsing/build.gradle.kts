import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.net.URI

plugins {
    kotlin("multiplatform")
    id("generated-sources")
}

repositories {
    maven {
        name = "IntellijDependencies"
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        d8()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
                implementation(libs.org.jetbrains.syntax.api)
            }
            kotlin {
                srcDir("common/src")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project(":compiler:psi"))
                implementation(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
                implementation(intellijCore())
                runtimeOnly(libs.intellij.fastutil)
                implementation(project(":compiler:test-infrastructure-utils"))
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)
                api(kotlinTest("junit"))
            }
            kotlin {
                srcDir("jvm/test")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    val testDataDirs = listOf(
        project(":compiler").isolated.projectDirectory.dir("testData"),
        project(":compiler:tests-spec").isolated.projectDirectory.dir("testData")
    ).joinToString(File.pathSeparator)
    systemProperty("test.data.dirs", testDataDirs)
}

val flexGeneratorClasspath: Configuration by configurations.creating

dependencies {
    flexGeneratorClasspath(commonDependency("org.jetbrains.intellij.deps.jflex", "jflex")) {
        // Flex brings many unrelated dependencies, so we are dropping them because only a flex `.jar` file is needed.
        // It can be probably removed when https://github.com/JetBrains/intellij-deps-jflex/issues/10 is fixed.
        isTransitive = false
    }
}

val skeletonVersion = "9fca651b6dc684ac340b45f5abf71cac6856aa45"
val skeletonFilePath: String = layout.buildDirectory.file("idea-flex-kotlin-$skeletonVersion.skeleton").get().asFile.absolutePath
val downloadSkeletonTaskName = "downloadSkeleton"

// TODO: KT-77206 (Get rid of the skeleton downloading or use JFlex version instead of the commit hash).
// The usage of permalink is confusing and might be not reliable.
// It's blocked by https://github.com/JetBrains/intellij-deps-jflex/issues/9
tasks.register(downloadSkeletonTaskName) {
    val skeletonFile = File(skeletonFilePath)

    onlyIf { !skeletonFile.exists() }

    val skeletonVersionString = skeletonVersion
    doFirst {
        val skeletonUrl =
            "https://raw.githubusercontent.com/JetBrains/intellij-community/$skeletonVersionString/tools/lexer/idea-flex-kotlin.skeleton"
        println("Downloading skeleton file $skeletonUrl")
        URI.create(skeletonUrl).toURL().openStream().use { input ->
            skeletonFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

for (lexerName in listOf("Kotlin", "KDoc")) {
    val taskName = "generate${lexerName}Lexer"
    generatedSourcesTask(
        taskName = taskName,
        generatorClasspath = flexGeneratorClasspath,
        generatorRoot = "compiler/multiplatform-parsing/common/src",
        generatorMainClass = "jflex.Main",
        argsProvider = { generationRoot ->
            listOf(
                projectDir.resolve("common/src/org/jetbrains/kotlin/kmp/lexer/$lexerName.flex").absolutePath,
                "-skel",
                skeletonFilePath,
                "-d",
                generationRoot.asFile.absolutePath,
                "--output-mode",
                "kotlin",
                "--nobak", // Prevent generating backup `.kt~` files
            )
        },
        commonSourceSet = true,
    ).configure {
        dependsOn(downloadSkeletonTaskName)
    }
}
