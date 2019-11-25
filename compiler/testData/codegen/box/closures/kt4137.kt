// IGNORE_BACKEND_FIR: JVM_IR
open class A(val s: Int) {

}

infix fun Int.foo(s: Int): Int {
    return this + s
}

open class B : A({ 1 foo 2} ())

fun box(): String {
    return if (B().s == 3) "OK" else "Fail"
}