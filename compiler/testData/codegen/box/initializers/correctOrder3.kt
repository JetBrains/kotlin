// ISSUE: KT-73355
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

// WITH_STDLIB
import kotlin.test.*

class Foo(initValue: Int) {
    init { setValue(initValue) }
    fun setValue(value: Int) { field = value }
    var field: Int = 0
}

fun box(): String {
    assertEquals(1, Foo(1).field)
    return "OK"
}
