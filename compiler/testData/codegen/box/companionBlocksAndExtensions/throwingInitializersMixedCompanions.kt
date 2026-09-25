// ISSUE: KT-87009
// LANGUAGE: +CompanionBlocks +CompanionExtensions

package foo

class MixedCompanions {
    companion {
        val before = "before"
    }

    companion object {
        val never: String = run {
            throw IllegalStateException("MixedCompanions.never")
        }

        fun objectEntry(): String = before
    }

    companion {
        val after = "after"

        fun blockEntry(): String = after
    }

    class Nested {
        fun read(): String = after
    }
}

private fun expectedInitializationFailureMessage(): String =
    when (BACKEND_UNDER_TEST) {
        "JS_IR", "JS_IR_ES6" -> "Could not initialize class MixedCompanions"
        else -> "Could not initialize class foo.MixedCompanions"
    }

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

fun box(): String {
    try {
        MixedCompanions.blockEntry()
        return "FAIL: mixed first access"
    } catch (e: Throwable) {
        assertExceptionInInitializerError(e, "mixed first access", "MixedCompanions.never")?.let { return it }
    }

    try {
        MixedCompanions.objectEntry()
        return "FAIL: mixed object entry"
    } catch (e: Throwable) {
        val expectedMessage = expectedInitializationFailureMessage()
        assertNoClassDefFoundError(e, "mixed object entry", expectedMessage)?.let { return it }
    }

    try {
        val companion = MixedCompanions.Companion
        return "FAIL: mixed companion access: $companion"
    } catch (e: Throwable) {
        val expectedMessage = expectedInitializationFailureMessage()
        assertNoClassDefFoundError(e, "mixed companion access", expectedMessage)?.let { return it }
    }

    try {
        MixedCompanions.Nested().read()
        return "FAIL: mixed nested access"
    } catch (e: Throwable) {
        val expectedMessage = expectedInitializationFailureMessage()
        assertNoClassDefFoundError(e, "mixed nested access", expectedMessage)?.let { return it }
    }

    return "OK"
}
