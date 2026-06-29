// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK

class C {
    companion object {
        val never: Nothing = run { throw IllegalStateException("C.never") }
    }
}

open class Parent {
    companion object {
        val never: Nothing = run { throw IllegalStateException("Child.never") }
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

class ThrowsErrorWithCompanion {
    companion object {
        val never: Nothing = run { throw Error("ThrowsErrorWithCompanion.never") }
    }
}

object ThrowsErrorObject {
    val never: Nothing = run { throw Error("ThrowsErrorObject.never") }
    fun foo() {}
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
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 2.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class C"
        if (e.message !=expectedMessage) return "FAIL 2.3: message must be '$expectedMessage', was '${e.message}'"
    }

    @Suppress("INVISIBLE_REFERENCE")
    try {
        Child()
        return "FAIL 3.1: should throw"
    } catch (e: ExceptionInInitializerError) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 3.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "Child.never") return "FAIL 3.3: message must be 'Child.never', was '${cause.message}'"
        if (e.message != null) return "FAIL 3.4: message must be null, got ${e.message}"
    }

    try {
        Child()
        return "FAIL 4.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 4.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class Child"
        if (e.message != expectedMessage) return "FAIL 4.3: message must be '$expectedMessage', was '${e.message}'"
    }

    try {
        Parent()
        return "FAIL 5.1: should throw"
    } catch (e: Throwable) {
        if (e.cause != null) return "FAIL 5.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class Parent"
        if (e.message != expectedMessage) return "FAIL 5.3: message must be '$expectedMessage', was '${e.message}'"
    }

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

    try {
        O.foo()
        return "FAIL 7.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 7.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class O"
        if (e.message != expectedMessage) return "FAIL 7.3: message must be '$expectedMessage', was '${e.message}'"
    }

    try {
        ThrowsErrorWithCompanion()
        return "FAIL 8.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 8.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsErrorWithCompanion.never") return "FAIL 8.3: message must be 'ThrowsErrorWithCompanion.never', was '${e.message}'"
    }

    try {
        ThrowsErrorWithCompanion()
        return "FAIL 9.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 9.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class ThrowsErrorWithCompanion"
        if (e.message != expectedMessage) return "FAIL 9.3: message must be '$expectedMessage', was '${e.message}'"
    }


    try {
        ThrowsErrorObject.foo()
        return "FAIL 10.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 10.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsErrorObject.never") return "FAIL 10.3: message must be 'ThrowsErrorObject.never', was '${e.message}'"
    }

    try {
        ThrowsErrorObject.foo()
        return "FAIL 11.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 11.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class ThrowsErrorObject"
        if (e.message != expectedMessage) return "FAIL 11.3: message must be '$expectedMessage', was '${e.message}'"
    }

    return "OK"
}
