// FIR_IDENTICAL
// SKIP_TXT
// WITH_STDLIB
// ISSUE: KT-56445

fun foo(x: Int?, y: Int = 1) {}
fun bar() {}

fun test() {
    mapOf( // String, KFunction<Unit>
        "a" to ::foo,
        "b" to ::bar,
    )
}
