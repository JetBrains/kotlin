// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
