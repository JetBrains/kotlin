// IGNORE_BACKEND: JS_IR
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME
// IGNORE_BACKEND: NATIVE

import kotlin.UninitializedPropertyAccessException

lateinit var str: String

fun box(): String {
    var str2: String = ""
    try {
        str2 = str
        return "Should throw an exception"
    }
    catch (e: UninitializedPropertyAccessException) {
        return "OK"
    }
    catch (e: Throwable) {
        return "Unexpected exception: ${e::class}"
    }
}