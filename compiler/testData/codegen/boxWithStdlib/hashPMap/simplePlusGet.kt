import kotlin.reflect.jvm.internal.pcollections.HashPMap
import kotlin.test.*

fun box(): String {
    var map = HashPMap.empty<String, Any>()!!

    map = map.plus("lol", 42)!!

    assertEquals(1, map.size())
    assertTrue(map.containsKey("lol"))
    assertFalse(map.containsKey(""))
    assertEquals(42, map["lol"])
    assertEquals(null, map[""])

    map = map.plus("", 0)!!

    assertEquals(2, map.size())
    assertTrue(map.containsKey("lol"))
    assertTrue(map.containsKey(""))
    assertEquals(42, map["lol"])
    assertEquals(0, map[""])

    return "OK"
}
