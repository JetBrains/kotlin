// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.jvm.*

enum class E

fun box(): String {
    try {
        val c = E::class.constructors.single()
        c.isAccessible = true
        c.call()
        return "Fail: constructing an enum class should not be allowed"
    }
    catch (e: Throwable) {
        return "OK"
    }
}
