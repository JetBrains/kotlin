// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.test.*
import kotlin.reflect.KClass

fun box(): String {
    val any = Array<Any>::class
    val string = Array<String>::class

    assertNotEquals<KClass<*>>(any, string)
    assertNotEquals<Class<*>>(any.java, string.java)

    return "OK"
}
