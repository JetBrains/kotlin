// !DIAGNOSTICS: -UNUSED_PARAMETER

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> foo(): @kotlin.internal.NoInfer T = TODO()

fun <K> bar(k: K) {}

fun test() {
    bar(foo<Int>())
}