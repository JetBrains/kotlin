import kotlin.test.*

class Klass
class Other

inline fun <reified T> simpleName(): String =
        T::class.simpleName!!

inline fun <reified T1, reified T2> twoReifiedParams(): String =
        "${T1::class.simpleName!!}, ${T2::class.simpleName!!}"

inline fun <reified T> myJavaClass(): Class<T> =
        T::class.java

fun box(): String {
    assertEquals("Klass", simpleName<Klass>())
    assertEquals("Int", simpleName<Int>())
    assertEquals("Array", simpleName<Array<Int>>())
    assertEquals("Error", simpleName<Error>())
    assertEquals("Klass, Other", twoReifiedParams<Klass, Other>())

    assertEquals(javaClass<String>(), myJavaClass<String>())
    assertEquals(javaClass<IntArray>(), myJavaClass<IntArray>())
    assertEquals(javaClass<Klass>(), myJavaClass<Klass>())
    assertEquals(javaClass<Error>(), myJavaClass<Error>())

    return "OK"
}
