// ISSUE: KT-79977, KT-79916
// WITH_STDLIB
// IGNORE_BACKEND_K1: NATIVE
// ^ Native test runner passes `-language-version 1.9`, and we use the UUID API here, which is @SinceKotlin("2.0").

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.1.0 2.2.0
// ^^^ KT-79916 fixed in 2.3.0-Beta1

// NOTE: Please keep the content of this file in sync with kt79977.kt!

// FILE: main.kt
// RECOMPILE

@file:OptIn(ExperimentalUuidApi::class)

import kotlin.test.assertEquals
import kotlin.uuid.*

// Prevent potential constant folding
fun rotateLeft(value: Long, shift: Int): Long {
    return value.rotateLeft(shift)
}

// Prevent potential constant folding
fun toRawBits(x: Double): Long = x.toRawBits()

// Prevent potential constant folding
fun fromRawBits(x: Long): Double = Double.fromBits(x)

fun box(): String {
    assertEquals(0x372ABAC_DEEF01237, rotateLeft(0x7372ABAC_DEEF0123, 4))
    assertEquals(4638387860618067575L, toRawBits(123.456))
    assertEquals(123.456, fromRawBits(4638387860618067575L))
    assertEquals("550e8400-e29b-41d4-a716-446655440000", Uuid.parseHex("550E8400e29b41d4A716446655440000").toString())
    return "OK"
}
