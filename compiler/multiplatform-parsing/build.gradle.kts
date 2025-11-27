import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.net.URI

plugins {
    kotlin("multiplatform")
    id("generated-sources")
    id("binaryen-configuration")
    id("nodejs-configuration")
    id("d8-configuration")
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
                implementation(libs.org.jetbrains.annotations)
            }
            kotlin {
                srcDir("common/src")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project(":compiler:psi:psi-api"))
                implementation(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
                implementation(intellijCore())
                runtimeOnly(libs.intellij.fastutil)
                runtimeOnly(commonDependency("com.fasterxml:aalto-xml"))
                implementation(project.dependencies.testFixtures(project(":compiler:test-infrastructure-utils")))
                implementation(project(":compiler:cli"))
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
        project(":compiler:tests-spec").isolated.projectDirectory.dir("testData"),
        project(":compiler:fir:analysis-tests").isolated.projectDirectory.dir("testData"),
        project(":analysis:analysis-api").isolated.projectDirectory.dir("testData"),
    ).joinToString(File.pathSeparator)
    systemProperty("test.data.dirs", testDataDirs)

    dependsOn(":createIdeaHomeForTests")
    systemProperty("idea.home.path", ideaHomePathForTests().get().asFile.canonicalPath)
}

val flexGeneratorDependencies = configurations.dependencyScope("flexGeneratorDependencies")
val flexGeneratorClasspath = configurations.resolvable("flexGeneratorClasspath") {
    extendsFrom(flexGeneratorDependencies.get())
}

dependencies {
    flexGeneratorDependencies.name(commonDependency("org.jetbrains.intellij.deps.jflex", "jflex")) {
        // Flex brings many unrelated dependencies, so we are dropping them because only a flex `.jar` file is needed.
        // It can be probably removed when https://github.com/JetBrains/intellij-deps-jflex/issues/10 is fixed.
        isTransitive = false
    }
}

val lexerGrammarsDirRelativeToRoot = layout.projectDirectory.dir("common/src/org/jetbrains/kotlin/kmp/lexer")

// TODO: KT-77206 (Get rid of the skeleton downloading or use JFlex version instead of the commit hash).
// The usage of permalink is confusing and might be not reliable.
// It's blocked by https://github.com/JetBrains/intellij-deps-jflex/issues/9
val skeletonDownloadTask = tasks.register("downloadSkeleton") {
    val skeletonVersion = "9fca651b6dc684ac340b45f5abf71cac6856aa45"
    val skeletonFile = layout.buildDirectory.file("idea-flex-kotlin-$skeletonVersion.skeleton")

    inputs.property("skeletonVersion", skeletonVersion)
    outputs.file(skeletonFile)

    doFirst {
        val skeletonFileOutput = skeletonFile.get().asFile
        skeletonFileOutput.parentFile.mkdirs()
        val skeletonUrl =
            "https://raw.githubusercontent.com/JetBrains/intellij-community/$skeletonVersion/tools/lexer/idea-flex-kotlin.skeleton"
        println("Downloading skeleton file $skeletonUrl")
        URI.create(skeletonUrl).toURL().openStream().use { input ->
            skeletonFileOutput.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

for (lexerName in listOf("KDoc", "Kotlin")) {
    val taskName = "generate${lexerName}Lexer"

    val lexerFile = lexerGrammarsDirRelativeToRoot.file("$lexerName.flex")
    generatedSourcesTask(
        taskName = taskName,
        generatorClasspath = flexGeneratorClasspath,
        generatorMainClass = "jflex.Main",
        argsProvider = { generationRoot ->
            listOf(
                lexerFile.asFile.absolutePath,
                "-skel",
                skeletonDownloadTask.get().outputs.files.singleFile.absolutePath,
                "-d",
                generationRoot.asFile.absolutePath,
                "--output-mode",
                "kotlin",
                "--nobak", // Prevent generating backup `.kt~` files
            )
        },
        commonSourceSet = true,
        additionalInputsToTrack = { fileCollection ->
            fileCollection.from(lexerFile)
            fileCollection.from(skeletonDownloadTask)
        }
    )
}
