// IGNORE_BACKEND_FIR: JVM_IR
fun test(i: Inv<out Any?>) {
    foo(i.superclass())
}

fun <T> foo(x: T) {}

class Inv<T>

fun <T> Inv<T>.superclass(): Inv<in T> = Inv()

fun box(): String {
    test(Inv())
    return "OK"
}