import kotlin.reflect.jvm.internal.pcollections.HashPMap
import kotlin.test.*

fun box(): String {
    var map = HashPMap.empty<String, Any>()!!

    map = map.plus("lol", 42)!!
    map = map.minus("lol")!!

    assertEquals(0, map.size())
    assertFalse(map.containsKey("lol"))
    assertEquals(null, map["lol"])

    map = map.plus("abc", "a")!!
    map = map.minus("abc")!!
    map = map.plus("abc", "d")!!

    assertEquals(1, map.size())
    assertTrue(map.containsKey("abc"))
    assertEquals("d", map["abc"])

    return "OK"
}
