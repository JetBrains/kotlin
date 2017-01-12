// TODO: Enable when JS backend gets support of Java class library
// IGNORE_BACKEND: JS
fun ok(b: Boolean) = if (b) "OK" else "Fail"

fun box(): String {
    val data = java.util.Arrays.asList("foo", "bar")!!
    return ok(data.contains("foo"))
}
