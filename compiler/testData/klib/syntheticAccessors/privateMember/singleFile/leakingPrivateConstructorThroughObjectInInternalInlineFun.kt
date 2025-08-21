// ISSUE: KT-80226
// IGNORE_BACKEND: NATIVE, JS_IR, WASM
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: NATIVE, JS_IR, WASM
// ^^^KT-80226: ClassCastException: class IrSimpleFunctionSymbolImpl cannot be cast to class IrConstructorSymbol

open class A(val x: Long) {
    private constructor(x: Int): this(x.toLong())

    internal inline fun plus1() = object : A(x.toInt() + 1) {}
}

fun box(): String {
    val result = A(1).plus1().x
    if (result != 2L) return result.toString()
    return "OK"
}