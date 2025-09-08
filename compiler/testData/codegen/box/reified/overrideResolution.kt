// ISSUE: KT-78321
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ Wrong box result: C C B B
// IGNORE_BACKEND: WASM
// ^^^ Wrong box result: C C make make

// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// after fixing everything here and in overrideResolutionWithInlinedFunInKlib.kt,
//   please delete overrideResolutionWithInlinedFunInKlib.kt and delete `LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization` above

fun box(): String {
    var result = ""
    result += (C as A<Int>).foo(42)
    result += " "
    result += (C as B).foo(42)
    result += " "
    result += makeA<Int>().foo(42)
    result += " "
    result += makeB<Int>().foo(42)
    if (result != "C C make B") return result
    return "OK"
}

interface A<T> {
    fun foo(t: T) = "A"
}

interface B {
    fun foo(t: Int) = "B"
}

interface AB : A<Int>, B {
    override fun foo(t: Int) = "C"
}

object C : AB

private inline fun <reified T> makeA(): A<T> = object : A<T>, B {
    override fun foo(t: T) = "make"
}

private inline fun <reified T> makeB(): B = object : A<T>, B {
    override fun foo(t: T) = "make"
}