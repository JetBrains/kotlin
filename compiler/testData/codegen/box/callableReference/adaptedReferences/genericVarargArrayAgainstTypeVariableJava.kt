// ISSUE: KT-90169
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

fun <T> applyJ(t: T, f: java.util.function.Function<T, Int>): Int = f.apply(t)
fun <E> countG(vararg xs: E): Int = xs.size

fun check(name: String, actual: Int, expected: Int = 2): String? =
    if (actual != expected) "FAIL $name: $actual" else null

fun box(): String {
    val arr = arrayOf("a", "b")
    val a = arrayOf<Any?>("a", "b")
    if (a.let(java.util.Objects::hash) != java.util.Objects.hash("a", "b")) return "FAIL Objects.hash"
    return check("Java Function", applyJ(arr, ::countG))
        ?: check("Arrays.asList", arr.let(java.util.Arrays::asList).size)
        ?: "OK"
}
