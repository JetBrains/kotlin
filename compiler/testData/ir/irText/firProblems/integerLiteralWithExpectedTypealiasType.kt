// FIR_IDENTICAL
// ISSUE: KT-56176

typealias MyLong = Long

fun foo(l: MyLong): String {
    return if (l == 0L) "OK" else "fail"
}

fun box(): String = foo(0)
