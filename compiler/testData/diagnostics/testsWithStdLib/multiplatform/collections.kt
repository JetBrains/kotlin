// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
// TARGET_PLATFORM: Common

import kotlin.collections.listOf

fun common() {
    listOf("foo", "bar").map { it }
}

// MODULE: jvm
// FILE: jvm.kt
// TARGET_PLATFORM: JVM

import kotlin.collections.mapOf

fun jvm() {
    mapOf(1 to "1")
}
