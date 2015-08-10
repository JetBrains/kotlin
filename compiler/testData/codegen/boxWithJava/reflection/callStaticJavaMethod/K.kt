import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun box(): String {
    val f = J::foo
    assertEquals(listOf(Integer.TYPE, javaClass<IntArray>(), javaClass<Array<Any>>()), f.parameters.map { it.type.javaType })
    assertEquals(javaClass<String>(), f.returnType.javaType)

    assertEquals("01A", f.call(0, intArrayOf(1), arrayOf("A")))

    return "OK"
}
