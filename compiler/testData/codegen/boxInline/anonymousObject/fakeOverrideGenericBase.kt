// LANGUAGE: -IrInlinerBeforeKlibSerialization
// SKIP_UNBOUND_IR_SERIALIZATION
// ^^^ KT-76547, KT-76592: With IR Inliner in pre-serialization, some type parameters are dumped as <unbound IrTypeParameterSymbolImpl> before serialization.
// After deserialization, they have valid name. This mismatch causes test to fail

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