// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS, NATIVE, WASM
// WITH_REFLECT

annotation private class Ann(val name: String)

class A {
    @Ann("OK")
    fun foo() {}
}

fun box(): String {
    val ann = A::class.members.single { it.name == "foo" }.annotations.single() as Ann
    return ann.name
}
