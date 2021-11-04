// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.test.*

class C {
    fun foo() {}
    var bar = "OK"
}

fun C.extFun(i: Int) {}

fun KParameter.check(name: String) {
    assertEquals(name, this.name!!)
    assertEquals(KParameter.Kind.VALUE, this.kind)
}

fun box(): String {
    val cFoo = C()::foo
    val cBar = C()::bar
    val cExtFun = C()::extFun

    assertEquals(0, cFoo.parameters.size)
    assertEquals(0, cBar.getter.parameters.size)
    assertEquals(1, cBar.setter.parameters.size)

    assertEquals(1, cExtFun.parameters.size)
    cExtFun.parameters[0].check("i")

    return "OK"
}
