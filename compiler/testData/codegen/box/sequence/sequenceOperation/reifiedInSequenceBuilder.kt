// TARGET_BACKEND: JVM
// WITH_STDLIB
inline fun <reified T> names(items: List<Any>): List<String> =
    sequence { for (i in items) if (i is T) yield(T::class.java.simpleName + i) }.toList()
inline fun <reified T> firstOf(items: List<Any>): T? = sequence { yieldAll(items) }.filter { it is T }.firstOrNull() as T?
inline fun <reified T> cast(items: List<Any>): List<T> = sequence { yieldAll(items) }.map { it as T }.toList()
fun box(): String {
    val failedCast = try { cast<Int>(listOf("x")).toString() } catch (e: ClassCastException) { "cce" }
    val r = "${names<String>(listOf(1, "a", "b"))} ${firstOf<Int>(listOf("x", 2, 3))} ${cast<String>(listOf("y"))} $failedCast"
    return if (r == "[Stringa, Stringb] 2 [y] cce") "OK" else "Fail: $r"
}
