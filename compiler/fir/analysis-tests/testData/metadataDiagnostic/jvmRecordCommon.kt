// METADATA_TARGET_PLATFORMS: JVM, JS
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-85626
// WITH_STDLIB
// JVM_TARGET: 17

@kotlin.jvm.JvmRecord
data class Vector(
    val x: Float,
    val y: Float,
)
