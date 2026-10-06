// TARGET_BACKEND: JVM
// WITH_REFLECT

// The abstract member of a fun interface obtained via reflection can be called on a SAM-converted lambda.

import kotlin.test.assertEquals

fun interface StringMapper {
    fun map(s: String): String
}

fun interface BiMapper<A, B, R> {
    fun map(a: A, b: B): R
}

fun box(): String {
    val map = StringMapper::class.members.single { it.name == "map" }
    assertEquals("[test]", map.call(StringMapper { "[$it]" }, "test"))

    val biMap = BiMapper::class.members.single { it.name == "map" }
    assertEquals("a1", biMap.call(BiMapper<String, Int, String> { a, b -> a + b }, "a", 1))

    return "OK"
}
