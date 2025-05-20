// KT-77372
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.reflect.KClass

fun box(): String {
    assertEquals("String", getKClass().simpleName)
    return "OK"
}

fun getKClass(): KClass<String> {
    val clazz = String::class
    return clazz
}