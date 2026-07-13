// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class Wrapper(val i: Int)

fun Wrapper.render(): String = "W:$i"
fun String.render(): String = this

fun box(): String {
    val w = Wrapper(42)
    val s = "OK"

    val r1: () -> String = w::render
    val r2: () -> String = s::render

    if (r1() != "W:42") return "FAIL r1: ${r1()}"
    if (r2() != "OK") return "FAIL r2: ${r2()}"

    return "OK"
}
