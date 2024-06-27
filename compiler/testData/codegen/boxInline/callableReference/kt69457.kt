// NO_CHECK_LAMBDA_INLINING
// ^^^ Suppress error message about non-inlined lambda in JVM bytecode inliner.

// This test demonstrates a situation when a reference to an inline function is not inlined.
// This needs to be fixed in IR inliner in KT-69457.
// The test itself is not muted because a special condition was added to `validateIrAfterInliningAllFunctions`.

// FILE: 1.kt
inline fun Int.toStr(): String = toChar().toString()

// FILE: 2.kt
fun box(): String {
    val o = 79::toStr // not inlined
    val k = 75.toStr() // inlined
    val ok = o() + k
    return ok
}
