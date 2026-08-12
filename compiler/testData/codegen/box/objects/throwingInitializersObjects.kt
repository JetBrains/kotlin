// ISSUE: KT-87009
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK

package foo

object O {
    val never: Nothing = run { throw IllegalStateException("O.never") }
    fun foo() {}
}

class MyError(message: String) : Error(message)

object ThrowsMyErrorObject {
    val never: Nothing = run { throw MyError("ThrowsMyErrorObject.never") }
    fun foo() {}
}

fun box(): String {
    @Suppress("INVISIBLE_REFERENCE")
    try {
        O.foo()
        return "FAIL 6.1: should throw"
    } catch (e: ExceptionInInitializerError) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 6.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "O.never") return "FAIL 6.3: message must be 'O.never', was '${cause.message}'"
        if (e.message != null) return "FAIL 6.4: message must be null, got ${e.message}"
    }

    @Suppress("INVISIBLE_REFERENCE")
    try {
        O.foo()
        return "FAIL 7.1: should throw"
    } catch (e: NoClassDefFoundError) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class O"
                else -> "Could not initialize class foo.O"
            }
            if (e.message != expectedMessage) return "FAIL 7.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    try {
        ThrowsMyErrorObject.foo()
        return "FAIL 10.1: should throw"
    } catch (e: MyError) {
        if (e.cause != null) return "FAIL 10.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsMyErrorObject.never") return "FAIL 10.3: message must be 'ThrowsMyErrorObject.never', was '${e.message}'"
    }

    @Suppress("INVISIBLE_REFERENCE")
    try {
        ThrowsMyErrorObject.foo()
        return "FAIL 11.1: should throw"
    } catch (e: NoClassDefFoundError) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class ThrowsMyErrorObject"
                else -> "Could not initialize class foo.ThrowsMyErrorObject"
            }
            if (e.message != expectedMessage) return "FAIL 11.3: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    return "OK"
}
