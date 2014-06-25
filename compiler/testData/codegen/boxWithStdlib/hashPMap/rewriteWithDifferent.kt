import kotlin.reflect.jvm.internal.pcollections.HashPMap
import kotlin.test.*

fun box(): String {
    var map = HashPMap.empty<String, Any>()!!

    map = map.plus("lol", 42)!!
    map = map.plus("lol", 239)!!

    assertEquals(1, map.size())
    assertTrue(map.containsKey("lol"))
    assertFalse(map.containsKey(""))
    assertEquals(239, map["lol"])
    assertEquals(null, map[""])

    map = map.plus("", 0)!!
    map = map.plus("", 2.71828)!!
    map = map.plus("lol", 42)!!
    map = map.plus("", 3.14)!!

    assertEquals(2, map.size())
    assertTrue(map.containsKey("lol"))
    assertTrue(map.containsKey(""))
    assertEquals(42, map["lol"])
    assertEquals(3.14, map[""])

    return "OK"
}
