// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.*

class Klass
class Other

inline fun <reified T : Any> simpleName(): String =
        T::class.simpleName!!

inline fun <reified T1 : Any, reified T2 : Any> twoReifiedParams(): String =
        "${T1::class.simpleName!!}, ${T2::class.simpleName!!}"

inline fun <reified T : Any> myJavaClass(): Class<T> =
        T::class.java

fun box(): String {
    assertEquals("Klass", simpleName<Klass>())
    assertEquals("Int", simpleName<Int>())
    assertEquals("Array", simpleName<Array<Int>>())
    assertEquals("Error", simpleName<Error>())
    assertEquals("Klass, Other", twoReifiedParams<Klass, Other>())

    assertEquals(String::class.java, myJavaClass<String>())
    assertEquals(IntArray::class.java, myJavaClass<IntArray>())
    assertEquals(Klass::class.java, myJavaClass<Klass>())
    assertEquals(Error::class.java, myJavaClass<Error>())

    return "OK"
}
