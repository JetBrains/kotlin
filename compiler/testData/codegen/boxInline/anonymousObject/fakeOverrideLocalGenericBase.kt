// TARGET_BACKEND: JS_IR, WASM, NATIVE
// IGNORE_BACKEND_K1: ANY
// NO_CHECK_LAMBDA_INLINING

// MODULE: lib
// FILE: lib.kt
package lib

open class A<T> {
    fun tostr(c: T) = c.toString()
}

inline fun inlineFun(): String {
    abstract class B<T, K> : A<T>() {
        fun fromstr(s: String, convert: (String) -> K): K = convert(s)
    }
    class C() : B<Char, Char>()

    return C().tostr('O') + C().fromstr("K") { it[0] }
}

// MODULE: main(lib)
// FILE: box.kt
fun box() = lib.inlineFun()