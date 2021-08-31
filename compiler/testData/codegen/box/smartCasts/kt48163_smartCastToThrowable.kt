// TARGET_BACKEND: JVM

import java.io.Serializable

fun bar(s: Serializable) {
    when (s) {
        is Exception -> throw s
        else -> Unit
    }
}

fun box(): String {
    try {
        bar(Exception("OK"))
        return "Fail"
    } catch (e: Exception) {
        return e.message!!
    }
}
