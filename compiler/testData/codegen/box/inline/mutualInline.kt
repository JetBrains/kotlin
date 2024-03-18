// KT-51298: compiler crashes with StackOverflowError. Possible fix: probably a new checker in IR Inliner, raising a diagnostic.
// Then, it would make sense to convert this test to Some kind of Diagnostic Test,
// however not for FIR diagnostics, but for Inliner diagnostics
// IGNORE_BACKEND: ANY

inline fun factorial1(n: Int): Int =
    // for test simplicity, `require(n>0)` is skipped
    if (n <= 1) 1
    else n * factorial2(n - 1)

inline fun factorial2(n: Int): Int =
    // for test simplicity, `require(n>0)` is skipped
    if (n <= 1) 1
    else n * factorial1(n - 1)

fun box(): String {
    val f1 = factorial1(1)
    val f2 = factorial1(2)
    val f3 = factorial1(3)
    val f4 = factorial1(4)
    val f5 = factorial1(5)
    val f6 = factorial1(6)
    if (f1 != 1) return "FAIL f1: $f1"
    if (f2 != 2) return "FAIL f2: $f2"
    if (f3 != 6) return "FAIL f3: $f3"
    if (f4 != 24) return "FAIL f4: $f4"
    if (f5 != 120) return "FAIL f5: $f5"
    if (f6 != 720) return "FAIL f6: $f6"
    return "OK"
}
