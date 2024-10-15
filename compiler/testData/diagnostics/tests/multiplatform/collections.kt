// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
// TARGET_PLATFORM: Common

import kotlin.collections.<!UNRESOLVED_REFERENCE!>listOf<!>

fun common() {
    <!UNRESOLVED_REFERENCE!>listOf<!>("foo", "bar").<!DEBUG_INFO_MISSING_UNRESOLVED!>map<!> { <!UNRESOLVED_REFERENCE!>it<!> }
}

// MODULE: jvm
// FILE: jvm.kt
// TARGET_PLATFORM: JVM

import kotlin.collections.mapOf

fun jvm() {
    mapOf(1 to "1")
}
