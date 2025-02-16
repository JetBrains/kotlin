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

import kotlin.collections.mapOf

fun jvm() {
    mapOf(1 to "1")
}
