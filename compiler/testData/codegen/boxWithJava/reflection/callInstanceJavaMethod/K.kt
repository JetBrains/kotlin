import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun box(): String {
    val f = J::foo
    assertEquals(
            listOf(javaClass<J>(), javaClass<IntArray>(), javaClass<Array<Any>>(), javaClass<Integer>()),
            f.parameters.map { it.type.javaType }
    )
    assertEquals(javaClass<String>(), f.returnType.javaType)

    assertEquals("01A2", f.call(J(0), intArrayOf(1), arrayOf("A"), 2))

    return "OK"
}
