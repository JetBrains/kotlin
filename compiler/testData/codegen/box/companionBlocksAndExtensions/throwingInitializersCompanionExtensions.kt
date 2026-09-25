// ISSUE: KT-87009
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// FILE: lib.kt
package foo

class ExtensionReceiver

companion val ExtensionReceiver.never: String = run {
    throw IllegalStateException("ExtensionReceiver.never")
}

companion val ExtensionReceiver.after: String = "after"

// FILE: main.kt
package foo

private fun assertExceptionInInitializerError(e: Throwable, label: String, expectedCauseMessage: String): String? {
    val name = e::class.simpleName
    if (name != "ExceptionInInitializerError") return "FAIL: $label type: $name"

    val cause = e.cause ?: return "FAIL: $label cause: null"
    val causeName = cause::class.simpleName
    if (causeName != "IllegalStateException") return "FAIL: $label cause: $causeName"
    if (cause.message != expectedCauseMessage) return "FAIL: $label message: ${cause.message}"
    if (e.message != null) return "FAIL: $label wrapper message: ${e.message}"

    return null
}

private fun assertNoClassDefFoundError(e: Throwable, label: String, expectedMessage: String): String? {
    val name = e::class.simpleName
    if (name != "NoClassDefFoundError") return "FAIL: $label type: $name"
    if (BACKEND_UNDER_TEST != "ANDROID" && e.message != expectedMessage) {
        return "FAIL: $label message: ${e.message}"
    }
    return null
}

private fun expectedInitializationFailureMessage(): String =
    when (BACKEND_UNDER_TEST) {
        "JVM_IR" -> "Could not initialize class foo.LibKt"
        else -> "Could not initialize file"
    }

fun box(): String {
    try {
        ExtensionReceiver.never
        return "FAIL: extension first access"
    } catch (e: Throwable) {
        assertExceptionInInitializerError(e, "extension first access", "ExtensionReceiver.never")?.let { return it }
    }

    try {
        ExtensionReceiver.never
        return "FAIL: extension second access"
    } catch (e: Throwable) {
        val expectedMessage = expectedInitializationFailureMessage()
        assertNoClassDefFoundError(e, "extension second access", expectedMessage)?.let { return it }
    }

    try {
        ExtensionReceiver.after
        return "FAIL: extension sibling access"
    } catch (e: Throwable) {
        val expectedMessage = expectedInitializationFailureMessage()
        assertNoClassDefFoundError(e, "extension sibling access", expectedMessage)?.let { return it }
    }

    return "OK"
}
