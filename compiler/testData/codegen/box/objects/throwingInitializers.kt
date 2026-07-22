// ISSUE: KT-87009
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// FULL_JDK

package foo

class C {
    companion object {
        val never: Nothing = run { throw IllegalStateException("C.never") }
    }
}

open class Parent {
    companion object {
        val never: Nothing = run { throw IllegalStateException("Parent.never") }
    }
}

class Child : Parent() {
    companion object {
        val normal = 42
    }
}

object O {
    val never: Nothing = run { throw IllegalStateException("O.never") }
    fun foo() {}
}

class MyError(message: String) : Error(message)

class ThrowsMyErrorWithCompanion {
    companion object {
        val never: Nothing = run { throw MyError("ThrowsMyErrorWithCompanion.never") }
    }
}

object ThrowsMyErrorObject {
    val never: Nothing = run { throw MyError("ThrowsMyErrorObject.never") }
    fun foo() {}
}

fun box(): String {
    try {
        C()
        return "FAIL 1.1: should throw"
    } catch (e: Error /* ExceptionInInitializerError */) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 1.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "C.never") return "FAIL 1.3: message must be 'C.never', was '${cause.message}'"
    }

    try {
        C()
        return "FAIL 2.1: should throw"
    } catch (e: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class C"
                else -> "Could not initialize class foo.C"
            }
            if (e.message != expectedMessage) return "FAIL 2.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    val childEIIE = try {
        Child()
        return "FAIL 3.1: should throw"
    } catch (e: Error /* ExceptionInInitializerError */) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 3.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "Parent.never") return "FAIL 3.3: message must be 'Parent.never', was '${cause.message}'"
        e
    }

    try {
        Child()
        return "FAIL 4.1: should throw"
    } catch (e: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class Child"
                else -> "Could not initialize class foo.Child"
            }
            if (e.message != expectedMessage) return "FAIL 4.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    try {
        Parent()
        return "FAIL 5.1: should throw"
    } catch (e: Throwable /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class Parent"
                else -> "Could not initialize class foo.Parent"
            }
            if (e.message != expectedMessage) return "FAIL 5.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    try {
        O.foo()
        return "FAIL 6.1: should throw"
    } catch (e: Error /* ExceptionInInitializerError */) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 6.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "O.never") return "FAIL 6.3: message must be 'O.never', was '${cause.message}'"
    }

    try {
        O.foo()
        return "FAIL 7.1: should throw"
    } catch (e: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class O"
                else -> "Could not initialize class foo.O"
            }
            if (e.message != expectedMessage) return "FAIL 7.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    try {
        ThrowsMyErrorWithCompanion()
        return "FAIL 8.1: should throw"
    } catch (e: MyError) {
        if (e.cause != null) return "FAIL 8.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsMyErrorWithCompanion.never") return "FAIL 8.3: message must be 'ThrowsMyErrorWithCompanion.never', was '${e.message}'"
    }

    try {
        ThrowsMyErrorWithCompanion()
        return "FAIL 9.1: should throw"
    } catch (e: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class ThrowsMyErrorWithCompanion"
                else -> "Could not initialize class foo.ThrowsMyErrorWithCompanion"
            }
            if (e.message != expectedMessage) return "FAIL 9.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    try {
        ThrowsMyErrorObject.foo()
        return "FAIL 10.1: should throw"
    } catch (e: MyError) {
        if (e.cause != null) return "FAIL 10.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsMyErrorObject.never") return "FAIL 10.3: message must be 'ThrowsMyErrorObject.never', was '${e.message}'"
    }

    try {
        ThrowsMyErrorObject.foo()
        return "FAIL 11.1: should throw"
    } catch (e: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class ThrowsMyErrorObject"
                else -> "Could not initialize class foo.ThrowsMyErrorObject"
            }
            if (e.message != expectedMessage) return "FAIL 11.3: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    return "OK"
}
