// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

interface Foo<in T> {
    val x: Int
    fun foo(x: T)
}

fun Foo<*>.testReceiver() = x

fun Foo<*>.testSmartCastOnExtensionReceiver() {
    this as Foo<String>
    foo("string")
}

fun testValueParameter(vp: Foo<*>) = vp.x
