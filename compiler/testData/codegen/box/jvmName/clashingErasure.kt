// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun <T> List<T>.foo() = "foo"

@JvmName("fooInt")
fun List<Int>.foo() = "fooInt"

fun box(): String {
    val strings = listOf("", "").foo()
    if (strings != "foo") return "Fail: $strings"

    val ints = listOf(1, 2).foo()
    if (ints != "fooInt") return "Fail: $ints"

    return "OK"
}
