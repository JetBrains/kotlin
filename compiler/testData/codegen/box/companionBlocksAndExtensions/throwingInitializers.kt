// ISSUE: KT-87009
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// LANGUAGE: +CompanionBlocks
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// FULL_JDK

package foo

class C {
    companion {
        val never: Nothing = run { throw IllegalStateException("C.never") }
    }
}

open class Parent {
    companion {
        val never: Nothing = run { throw IllegalStateException("Parent.never") }
    }
}

class Child : Parent() {
    companion {
        val normal = 42
    }
}

class MyError(message: String) : Error(message)

class ThrowsMyError {
    companion {
        val never: Nothing = run { throw MyError("ThrowsMyError.never") }
    }
}

fun box(): String {
    @Suppress("INVISIBLE_REFERENCE")
    try {
        C()
        return "FAIL 1.1: should throw"
    } catch (e: ExceptionInInitializerError) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 1.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "C.never") return "FAIL 1.3: message must be 'C.never', was '${cause.message}'"
        if (e.message != null) return "FAIL 1.4: message must be null, got ${e.message}"
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

    @Suppress("INVISIBLE_REFERENCE")
    val childEIIE = try {
        Child()
        return "FAIL 3.1: should throw"
    } catch (e: ExceptionInInitializerError) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 3.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "Parent.never") return "FAIL 3.3: message must be 'Parent.never', was '${cause.message}'"
        if (e.message != null) return "FAIL 3.4: message must be null, got ${e.message}"
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
        ThrowsMyError()
        return "FAIL 6.1: should throw"
    } catch (e: MyError) {
        if (e.cause != null) return "FAIL 6.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsMyError.never") return "FAIL 6.3: message must be 'ThrowsMyError.never', was '${e.message}'"
    }

    try {
        ThrowsMyError()
        return "FAIL 7.1: should throw"
    } catch (e: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "NATIVE" -> "There was an error during file or class initialization"
                "JS_IR", "JS_IR_ES6" -> "Could not initialize class ThrowsMyError"
                else -> "Could not initialize class foo.ThrowsMyError"
            }
            if (e.message != expectedMessage) return "FAIL 7.2: message must be '$expectedMessage', was '${e.message}'"
        }
    }

    return "OK"
}
