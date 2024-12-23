// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

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
