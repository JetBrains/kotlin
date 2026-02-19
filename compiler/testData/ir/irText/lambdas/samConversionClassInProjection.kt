// TARGET_BACKEND: JVM_IR
// SAM_CONVERSIONS: CLASS
fun interface Cmp<T> {
    fun compare(a: T, b: T): Int
}

fun <T> foo(comparator: Cmp<in T>, x: T) {}

fun bar() = foo({ a, b -> a - b }, 1)