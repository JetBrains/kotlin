// DISABLE_IR_VISIBILITY_CHECKS: ANY
// LANGUAGE: +CompanionBlocksAndExtensions
// FULL_JDK

class C {
    companion {
        val never: Nothing = run { throw IllegalStateException("C.never") }
    }
}

open class Parent {
    companion {
        val never: Nothing = run { throw IllegalStateException("Child.never") }
    }
}

class Child : Parent() {
    companion {
        val normal = 42
    }
}

class ThrowsError {
    companion {
        val never: Nothing = run { throw Error("ThrowsError.never") }
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

    @Suppress("INVISIBLE_REFERENCE")
    try {
        C()
        return "FAIL 2.1: should throw"
    } catch (e: NoClassDefFoundError) {
        if (e.cause != null) return "FAIL 2.2: cause must be null, got ${e.cause}"
        val expectedMessage = "Could not initialize class C"
        if (e.message != expectedMessage) return "FAIL 2.3: message must be '$expectedMessage', was '${e.message}'"
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

    @Suppress("INVISIBLE_REFERENCE")
    try {
        Child()
        return "FAIL 4.1: should throw"
    } catch (e: NoClassDefFoundError) {
        if (e.cause != null) return "FAIL 4.2: cause must be null, got ${e.cause}"
        val expectedMessage = when (BACKEND_UNDER_TEST) {
            "NATIVE" -> "Could not initialize class Parent" // Slight difference in behavior in Native
            else -> "Could not initialize class Child"
        }
        if (e.message != expectedMessage) return "FAIL 4.3: message must be '$expectedMessage', was '${e.message}'"
    }

    @Suppress("INVISIBLE_REFERENCE")
    try {
        Parent()
        return "FAIL 5.1: should throw"
    } catch (e: NoClassDefFoundError) {
        if (e.cause != null) return "FAIL 5.2: cause must be null, got ${e.cause}"
        val expectedMessage = "Could not initialize class Parent"
        if (e.message != expectedMessage) return "FAIL 2.3: message must be '$expectedMessage', was '${e.message}'"
    }

    try {
        ThrowsError()
        return "FAIL 6.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 6.2: cause must be null, got ${e.cause}"
        if (e.message != "ThrowsError.never") return "FAIL 6.3: message must be 'ThrowsError.never', was '${e.message}'"
    }

    @Suppress("INVISIBLE_REFERENCE")
    try {
        ThrowsError()
        return "FAIL 7.1: should throw"
    } catch (e: NoClassDefFoundError) {
        if (e.cause != null) return "FAIL 7.2: cause must be null, got ${e.cause}"
        val expectedMessage = "Could not initialize class ThrowsError"
        if (e.message != expectedMessage) return "FAIL 7.3: message must be '$expectedMessage', was '${e.message}'"
    }

    return "OK"
}
