// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL
fun test1(d: dynamic) = d.member(1, 2, 3)

fun test2(d: dynamic) = d?.member(1, 2, 3)

// Named arguments in dynamic calls are ignored by JS back-end.
// If this becomes a compilation error, simply remove this particular test case.
fun test3(d: dynamic) = d.member(a = 1, b = 2, c = 3)
