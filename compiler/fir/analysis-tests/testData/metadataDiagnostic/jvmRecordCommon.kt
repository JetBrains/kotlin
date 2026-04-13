// METADATA_TARGET_PLATFORMS: JVM, JS
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-85626
// WITH_STDLIB
// JVM_TARGET: 17

@kotlin.jvm.JvmRecord
data <!MISSING_DEPENDENCY_SUPERCLASS("java.lang.Record; Vector")!>class Vector<!>(
    <!MISSING_DEPENDENCY_SUPERCLASS("java.lang.Record; Vector")!>val x: Float<!>,
    <!MISSING_DEPENDENCY_SUPERCLASS!>val y: Float<!>,
)
