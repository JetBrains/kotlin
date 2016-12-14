// !LANGUAGE: +MultiPlatformProjects
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

header fun external()
header fun tailrec()
header fun inline()
header fun String.unaryMinus(): String
header fun String.and(other: String): String

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl external fun external()
impl tailrec fun tailrec(): Unit = if (true) Unit else tailrec()
impl inline fun inline() {}
impl operator fun String.unaryMinus(): String = this
impl infix fun String.and(other: String): String = this + other
