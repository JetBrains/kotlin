import kotlin.test.assertEquals

fun String.sum(other: String = "b") = this + other

fun box(): String {
    val f = String::sum
    assertEquals("ab", f.callBy(mapOf(f.parameters.first() to "a")))
    return "OK"
}
