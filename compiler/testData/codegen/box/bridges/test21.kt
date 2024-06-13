// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

open class Base<T> {
    open var x: T? = null
}

open class Derived : Base<String>() {
    // override fun <get-x>: String? = super.<get-x> as String?
    // override fun <set-x>(value: String?) = super.<set-x>(value) // no bridge is needed
}

class Data(val x: Int)

fun garble(d: Derived) {
    (d as Base<Data>).x = Data(42)
}

fun box(): String {
    val d = Derived()
    garble(d)
    try {
        val x = d.x
        return "FAIL: ${x?.length}"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
