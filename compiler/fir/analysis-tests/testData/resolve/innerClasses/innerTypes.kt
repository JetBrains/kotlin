class Outer<T> {
    inner class Inner<K>
}

class Boxed<Q> {
    fun substitute() = Outer<Q>().Inner<Int>()
}

fun accept(p: Outer<String>.Inner<Int>) {}

val rr = Outer<String>().Inner<Int>()
val rrq = Boxed<String>().substitute()

fun check() {
    accept(<!ARGUMENT_TYPE_MISMATCH!>Outer<Int>().Inner<Int>()<!>) // illegal
    accept(<!ARGUMENT_TYPE_MISMATCH!>Outer<String>().Inner<String>()<!>) // illegal
    accept(Outer<String>().Inner<Int>()) // ok

    accept(<!ARGUMENT_TYPE_MISMATCH!>Boxed<Int>().substitute()<!>) // illegal
    accept(Boxed<String>().substitute()) // ok
}
