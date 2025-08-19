// KT-49722
// Right now there will be an internal compiler error, since inliner lowering is not ready for such a code
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE, WASM
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// ^^^ in deserialization tests, IR Inliner on 1st stage crashes before IR dumps comparison. So let's test dumps without inliner

interface I<T> {
    val prop: T
    fun h(x: T = prop): Any
}

interface I2<T> : I<String>

open class A<T> {
    inline fun h(x: String) = x
}

class B : A<Int>(), I2<Int> {
    override val prop get() = "OK"
}

fun box() = B().h()