// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

fun String?.foo(x: Int, y: Array<Int>, z: IntArray, w: List<Map<Any, A<*>>>) {}

class A<T> {
    fun <U> bar(t: T, u: U): T? = null
}

fun baz(inProjection: A<in Number>, outProjection: A<out Number>) {}

fun box(): String {
    assertEquals(
            listOf(
                    "kotlin.String?",
                    "kotlin.Int",
                    "kotlin.Array<kotlin.Int>",
                    "kotlin.IntArray",
                    "kotlin.collections.List<kotlin.collections.Map<kotlin.Any, A<*>>>"
            ),
            String?::foo.parameters.map { it.type.toString() }
    )

    assertEquals("kotlin.Unit", String?::foo.returnType.toString())

    val bar = A::class.members.single { it.name == "bar" }
    assertEquals(listOf("A<T>", "T", "U"), bar.parameters.map { it.type.toString() })
    assertEquals("T?", bar.returnType.toString())

    assertEquals(
            listOf("A<in kotlin.Number>", "A<out kotlin.Number>"),
            ::baz.parameters.map { it.type.toString() }
    )

    return "OK"
}
