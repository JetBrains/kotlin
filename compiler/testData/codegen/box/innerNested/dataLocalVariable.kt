// TARGET_BACKEND: JVM
fun ok(b: Boolean) = if (b) "OK" else "Fail"

fun box(): String {
    val data = java.util.Arrays.asList("foo", "bar")!!
    return ok(data.contains("foo"))
}
