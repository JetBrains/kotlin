// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit): Unit = block()

fun test() {
    myRun { throw java.lang.IllegalArgumentException() }
    val x: Int = 42
}