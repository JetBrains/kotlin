package KotlinxTrainProjectK2

import KotlinxTrainProjectK2.buildTypes.*
import KotlinxTrainProjectK2.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("KotlinxTrainProjectK2")
    name = "Kotlinx Train K2"
    description = "Building kotlinx train with kotlin K2 compiler"

    vcsRoot(kotlinxatomicfutrain)

    buildType(KotlinxTrainCoroutinesBuildAndTestsK2)
    buildType(KotlinxTrainKtorK2)
    buildType(KotlinxTrainCoroutinesK2MavenArtifactsOnly)
    buildType(KotlinxTrainAtomicfuK2)
    buildType(KotlinxTrainSerializationK2)

    params {
        text("reverse.dep.*.kotlin_snapshot_version", "2.1.0", description = "see https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/org/jetbrains/kotlin/kotlin-compiler/", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }
})
