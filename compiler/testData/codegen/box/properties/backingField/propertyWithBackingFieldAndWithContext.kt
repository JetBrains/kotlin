// LANGUAGE: +ExplicitBackingFields
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM
// LANGUAGE: +ContextParameters

class OverloadWithContext {
    val a: Any
        field: String = "not OK"

    context(a: OverloadWithContext)
    val a: String
        get() = "OK"

    fun acceptString(a: String): String {
        return a
    }

    fun usage(): String {
        return acceptString(a)
    }
}

fun box(): String {
    return OverloadWithContext().usage()
}