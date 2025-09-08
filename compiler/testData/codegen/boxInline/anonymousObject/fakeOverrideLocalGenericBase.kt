// TARGET_BACKEND: JS_IR, WASM
// IGNORE_BACKEND_K1: ANY
// NO_CHECK_LAMBDA_INLINING
// IGNORE_FIR_DIAGNOSTICS
// This code actually triggers `NOT_YET_SUPPORTED_IN_INLINE` but the test may still be useful to track the way the compiler works.

// MODULE: lib
// FILE: lib.kt
package lib

open class A<T> {
    fun tostr(c: T) = c.toString()
}

inline fun inlineFun(): String {
    abstract <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> B<T, K> : A<T>() {
        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> fromstr(s: String, convert: (String) -> K): K = convert(s)
    }
    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> C() : B<Char, Char>()

    return C().tostr('O') + C().fromstr("K") { it[0] }
}

// MODULE: main(lib)
// FILE: box.kt
fun box() = lib.inlineFun()
