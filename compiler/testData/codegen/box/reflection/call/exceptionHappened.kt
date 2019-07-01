// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.InvocationTargetException

fun fail(message: String) {
    throw AssertionError(message)
}

fun box(): String {
    try {
        ::fail.call("OK")
    } catch (e: InvocationTargetException) {
        return e.getTargetException().message.toString()
    }

    return "Fail: no exception was thrown"
}
