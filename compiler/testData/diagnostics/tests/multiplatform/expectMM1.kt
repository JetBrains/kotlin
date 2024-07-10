// FIR_IDENTICAL
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.coroutines.*

internal expect class UndispatchedCoroutine<in T>(
    uCont: Continuation<T>
)

// MODULE: m2-jvm()()(m1-common)
// FILE: int2.kt

import kotlin.coroutines.*

internal actual class UndispatchedCoroutine<in T>actual constructor (
    uCont: Continuation<T>
)

// MODULE: m3-jvm()()(m1-common, m2-jvm)
// FILE: jvm.kt

fun main() {}
