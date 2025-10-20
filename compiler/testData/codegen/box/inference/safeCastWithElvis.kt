// WITH_STDLIB
// ISSUE: KT-62806

fun <T: Number> foo(x: Number) = x as? T ?: TODO()
val x: Int? = foo<Int>(1)

fun box(): String {
    return if (x == 1) "OK" else "Fail: $x"
}
