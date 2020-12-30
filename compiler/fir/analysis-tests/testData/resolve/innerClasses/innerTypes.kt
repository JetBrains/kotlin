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
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>accept<!>(Outer<Int>().Inner<Int>())<!> // illegal
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>accept<!>(Outer<String>().Inner<String>())<!> // illegal
    accept(Outer<String>().Inner<Int>()) // ok

    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>accept<!>(Boxed<Int>().substitute())<!> // illegal
    accept(Boxed<String>().substitute()) // ok
}
