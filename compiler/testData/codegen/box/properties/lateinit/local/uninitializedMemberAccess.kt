// IGNORE_BACKEND: JS_IR
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME
// IGNORE_BACKEND: NATIVE

import kotlin.UninitializedPropertyAccessException

fun box(): String {
    lateinit var str: String
    var i: Int = 0
    try {
        i = str.length
        return "Should throw an exception"
    }
    catch (e: UninitializedPropertyAccessException) {
        return "OK"
    }
    catch (e: Throwable) {
        return "Unexpected exception: ${e::class}"
    }
}