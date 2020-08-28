// FIR_IDENTICAL
// !LANGUAGE: +NewInference

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