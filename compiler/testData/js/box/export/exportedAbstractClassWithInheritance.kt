// KT-64927
// TARGET_BACKEND: JS_IR

// MODULE: main
// FILE: main.kt

fun <T : Any> MutableVal(value: T): MutableVal<T> =
    MutableValImpl(value)

private class MutableValImpl<T : Any>(
    override var value: T,
) : MutableVal<T>()

@JsExport
abstract class Val<out T : Any> {
    abstract val value: T
}

abstract class MutableVal<T : Any> : Val<T>() {
    abstract override var value: T
}

fun box(): String {
    val value = MutableVal(1)
    if (value.value != 1) return "Fail: Unexpected initial value of field `value`"
    value.value = 2
    if (value.value != 2) return "Fail: Unexpected value of field `value` after mutation"
    return "OK"
}