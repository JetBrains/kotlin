package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.*

object KotlinxLibrariesCompilation : BuildType({
    name = "Kotlinx libraries compilation"
    description = "If the build failure is not led by the regression in kotlin compiler: https://jetbrains.team/p/kti/documents/All/a/Merge-Request-to-Kotlin-Repository-Process-community-projects-co#kotlinx-libraries-compilation-fix-process"

    type = BuildTypeSettings.Type.COMPOSITE
    buildNumberPattern = "${BuildNumber.depParamRefs.buildNumber}  (%build.counter%)"

    dependencies {
        snapshot(Tests_Linux.buildTypes.kotlinxAtomicfu) {
        }
        snapshot(Tests_Linux.buildTypes.kotlinxcoroutines) {
        }
    }
})
