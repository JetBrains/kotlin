// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: J.kt

import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReference

fun testJavaReference() {
    val ji = AtomicReference<Int>(1)
    val jji: AtomicReference<Int>
    jji = ji

    val jl = AtomicReference<Long>(1L)
    val jjl: AtomicReference<Long>
    jjl = jl

    val jb = AtomicReference<Char>('c')
    val jjb: AtomicReference<Char>
    jjb = jb
}

fun testJavaArray() {
    val ji = AtomicReferenceArray<Int>(1)
    val jji: AtomicReferenceArray<Int>
    jji = ji

    val jl = AtomicReferenceArray<Long>(1)
    val jjl: AtomicReferenceArray<Long>
    jjl = jl

    val jb = AtomicReferenceArray<Boolean>(1)
    val jjb: AtomicReferenceArray<Boolean>
    jjb = jb
}

// FILE: K.kt

@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.AtomicArray

fun testKotlinReference() {
    val ki = AtomicReference(127)
    val kki: AtomicReference<Int>
    kki = ki

    val kl = AtomicReference(127L)
    val kkl: AtomicReference<Long>
    kkl = kl

    val kb = AtomicReference(false)
    val kkb: AtomicReference<Boolean>
    kkb = kb
}

fun testKotlinArray() {
    val ki = AtomicArray(arrayOf(127))
    val kki: AtomicArray<Int>
    kki = ki

    val kl = AtomicArray(arrayOf(127L))
    val kkl: AtomicArray<Long>
    kkl = kl

    val kb = AtomicArray(arrayOf(false))
    val kkb: AtomicArray<Boolean>
    kkb = kb
}
