// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface Cmp<T> {
    fun compare(a: T, b: T): Int
}

fun <T> foo(comparator: Cmp<in T>, a: T, b: T) = comparator.compare(a, b)

fun bar(x: Int, y: Int) = foo({ a, b -> a - b}, x, y)

fun box(): String {
    val t = bar(42, 117)
    if (t != -75)
        return "Failed: t=$t"
    return "OK"
}
