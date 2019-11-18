// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

class L<T>(var a: T) {}

fun foo() = L<Int>(5).a

fun box(): String {
    val x: Any = foo()
    return if (x is Integer) "OK" else "Fail $x"
}
