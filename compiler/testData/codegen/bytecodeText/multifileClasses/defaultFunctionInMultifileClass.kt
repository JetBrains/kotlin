// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: part1.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

fun foo(o: String = "O", k: String = "K"): String = o + k

// FILE: part2.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

val bar = ""

// Default argument handling should not happen in the facade, only in parts.
// @Facade.class:
// 0 LDC
