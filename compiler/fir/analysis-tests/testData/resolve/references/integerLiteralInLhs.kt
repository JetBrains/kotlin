// FIR_DISABLE_LAZY_RESOLVE_CHECKS
fun Short.foo(): Int = 1
fun Int.foo(): Int = 2

fun testRef(f: () -> Int) {}

fun test() {
    // should resolve to Int.foo
    testRef(1::foo)
}
