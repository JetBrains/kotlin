// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM
// IGNORE_IR_DESERIALIZATION_TEST: ANY
// ISSUE: KT-52002

class Scope(val name: String)

interface Interface {
    fun foo(): String

    context(Scope)
    fun foo(): String
}

class ClassBoth : Interface {
    override fun foo() = "O"

    context(Scope)
    override fun foo() = "K"
}

fun box(): String {
    val scope = Scope("")
    val c = ClassBoth()
    val result = c.foo() + with(scope) { c.foo() }
    return result
}
