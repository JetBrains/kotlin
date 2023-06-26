class L<T>(var a: T) {}

fun foo() = L<Int>(5).a

fun box(): String {
    val x: Any = foo()
    return if (x is Int) "OK" else "Fail $x"
}
