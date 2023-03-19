// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: NATIVE
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <T> takeT(t: T) {}
inline fun <T : Any> takeTSuperAny(t: T) {}
inline fun <T, U : T> takeU(u: U) {}
inline fun <T, U : T, R : U> takeR(r: R) {}
inline fun <A, B, T : Map<A, List<B>>> takeTWithMap(t: T) {}

// FILE: 2.kt
import test.*

fun box(): String {
    val f = { null } as () -> Int
    takeT(f())
    // Without fix we are going to get following instructions
    //    CHECKCAST java/lang/Number
    //    INVOKEVIRTUAL java/lang/Number.intValue ()I    // <- this one leads to NPE
    takeTSuperAny(f())
    takeU(f())
    takeR(f())

    val g = { null } as () -> Map<Int, List<String>>
    takeTWithMap(g())
    return "OK"
}
