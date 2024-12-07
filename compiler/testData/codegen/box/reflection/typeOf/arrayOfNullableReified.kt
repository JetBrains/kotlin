// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_REFLECT

import kotlin.reflect.typeOf
import kotlin.test.assertEquals

inline fun <reified T> typeOfArrayOfNArrayOf() =
    typeOf<Array<Array<T>?>>()

inline fun <reified T> myTypeOf() =
    typeOf<T>()

inline fun <reified T> myTypeOfArrayOfNArrayOf() =
    typeOf<Array<Array<T>?>>()

fun box(): String {
    assertEquals(typeOf<Array<Array<String>?>>(), typeOfArrayOfNArrayOf<String>())
    assertEquals(typeOf<Array<Array<String>?>>(), myTypeOf<Array<Array<String>?>>())
    assertEquals(typeOf<Array<Array<String>?>>(), myTypeOfArrayOfNArrayOf<String>())
    return "OK"
}
