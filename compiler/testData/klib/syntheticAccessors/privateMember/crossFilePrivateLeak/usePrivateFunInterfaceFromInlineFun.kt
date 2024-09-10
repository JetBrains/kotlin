// IGNORE_BACKEND: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681.

// FILE: I.kt
private fun interface I {
    fun foo(): Int
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
inline fun publicInlineFun(): Int = (I { 1 }).foo()

@Suppress("PRIVATE_CLASS_MEMBER_FROM_INLINE")
internal inline fun internalInlineFun(): Int = (I { 1 }).foo()

// FILE: main.kt
fun box(): String {
    var result = 0
    result += publicInlineFun()
    result += internalInlineFun()
    return if (result == 2) "OK" else result.toString()
}
