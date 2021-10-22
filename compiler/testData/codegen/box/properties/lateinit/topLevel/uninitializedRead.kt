// WITH_RUNTIME

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
