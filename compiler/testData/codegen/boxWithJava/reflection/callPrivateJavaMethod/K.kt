import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.*

fun box(): String {
    val c = J::class.constructors.single()
    assertFalse(c.isAccessible)
    failsWith(javaClass<IllegalCallableAccessException>()) { c.call("") }

    c.isAccessible = true
    assertTrue(c.isAccessible)
    val j = c.call("OK")

    val m = J::class.members.single { it.name == "getResult" }
    assertFalse(m.isAccessible)
    failsWith(javaClass<IllegalCallableAccessException>()) { m.call(j)!! }

    m.isAccessible = true
    return m.call(j) as String
}
