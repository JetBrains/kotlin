// LANGUAGE: +IrInlinerBeforeKlibSerialization
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

// NO_CHECK_LAMBDA_INLINING

// MODULE: lib
// FILE: lib.kt
package lib

open class C<T> {
    fun tostr(c: T) = c.toString()
    inline fun fromstr(s: String, convert: (String) -> T): T = convert(s)
}

inline fun inlineFun(): String {
    val cc = object : C<Char>() {}
    return cc.tostr('O') + cc.fromstr("K") { it[0] }
}

// MODULE: main(lib)
// FILE: box.kt
fun box() = lib.inlineFun()