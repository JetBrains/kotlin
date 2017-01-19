// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FULL_JDK

import kotlin.test.assertEquals
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

open class TypeLiteral<T> {
    val type: Type
        get() = (javaClass.getGenericSuperclass() as ParameterizedType).getActualTypeArguments()[0]
}

inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}

fun box(): String {
    assertEquals("class java.lang.String", typeLiteral<String>().type.toString())
    assertEquals("java.util.List<?>", typeLiteral<List<*>>().type.toString())
    assertEquals("java.lang.String[]", typeLiteral<Array<String>>().type.toString())
    assertEquals("java.lang.Integer[]", typeLiteral<Array<Int>>().type.toString())
    assertEquals("java.lang.String[][]", typeLiteral<Array<Array<String>>>().type.toString())
    return "OK"
}
