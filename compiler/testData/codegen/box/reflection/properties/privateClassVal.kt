// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

class Result {
    private val value = "OK"

    fun ref() = Result::class.memberProperties.single() as KProperty1<Result, String>
}

fun box(): String {
    val p = Result().ref()
    try {
        p.get(Result())
        return "Fail: private property is accessible by default"
    } catch(e: IllegalCallableAccessException) { }

    p.isAccessible = true

    val r = p.get(Result())

    p.isAccessible = false
    try {
        p.get(Result())
        return "Fail: setAccessible(false) had no effect"
    } catch(e: IllegalCallableAccessException) { }

    return r
}
