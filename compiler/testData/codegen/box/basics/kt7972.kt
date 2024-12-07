// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// WITH_STDLIB

fun <E> List<E>.addAnything(element: E) {
    if (this is MutableList<E>) {
        this.add(element)
    }
}

fun box(): String {
    val list = arrayListOf(1, 2)
    list.addAnything("string")
    try {
        val x = list[2]
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
