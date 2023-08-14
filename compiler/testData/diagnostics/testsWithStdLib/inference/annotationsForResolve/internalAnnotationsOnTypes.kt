//!DIAGNOSTICS: -UNUSED_PARAMETER

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <R> Iterable<*>.filterIsInstance1(): List<@kotlin.internal.NoInfer R> = throw Exception()

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <R> List<*>.filterIsInstance2(): @kotlin.internal.NoInfer List<R> = throw Exception()

fun test(list: List<Int>) {
    list.filterIsInstance1<Int>().map { it * 2 }
    list.filterIsInstance2<Int>().filter { it > 10 }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <R> foo(t: R): List<@kotlin.internal.NoInfer R> = throw Exception("$t")

fun test() {
    foo(1).map { it * 2 }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <R> List<R>.foo(): @kotlin.internal.NoInfer R = throw Exception()

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <R> bar(r: R, f: Function1<@kotlin.internal.NoInfer R, Unit>): Nothing = throw Exception()

fun test1() {
    listOf("").foo().length
    bar(1) { x -> x + 1 }
}
