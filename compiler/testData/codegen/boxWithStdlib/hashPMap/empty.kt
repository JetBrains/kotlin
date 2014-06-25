import kotlin.reflect.jvm.internal.pcollections.HashPMap
import kotlin.test.*

fun box(): String {
    val map = HashPMap.empty<String, Any>()!!

    assertEquals(0, map.size())

    assertFalse(map.containsKey(""))
    assertFalse(map.containsKey("abacaba"))
    assertEquals(null, map[""])
    assertEquals(null, map["lol"])

    // Check that doesn't create a new map
    assertEquals(map, map.minus(""))

    // Check that all empty()s are equal
    val other = HashPMap.empty<String, Any>()!!
    assertEquals(map, other)

    return "OK"
}
