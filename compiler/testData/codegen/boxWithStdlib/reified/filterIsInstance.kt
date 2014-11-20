import kotlin.test.assertEquals

inline fun<reified T> Array<Any>.filterIsInstance(): List<T> {
    return this.filter { it is T }.map { it as T }
}

fun box(): String {
    val src: Array<Any> = array(1,2,3.toDouble(), "abc", "cde")

    assertEquals(arrayListOf(1,2), src.filterIsInstance<Int>())
    assertEquals(arrayListOf(3.0), src.filterIsInstance<Double>())
    assertEquals(arrayListOf("abc", "cde"), src.filterIsInstance<String>())
    assertEquals(src.toList(), src.filterIsInstance<Any>())

    return "OK"
}
