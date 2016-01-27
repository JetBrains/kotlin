import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.*

fun box(): String {
    val c = J::class.constructors.single()
    assertFalse(c.isAccessible)
    assertFailsWith(IllegalCallableAccessException::class) { c.call("") }

    c.isAccessible = true
    assertTrue(c.isAccessible)
    val j = c.call("OK")

    val m = J::class.members.single { it.name == "getResult" }
    assertFalse(m.isAccessible)
    assertFailsWith(IllegalCallableAccessException::class) { m.call(j)!! }

    m.isAccessible = true
    return m.call(j) as String
}
