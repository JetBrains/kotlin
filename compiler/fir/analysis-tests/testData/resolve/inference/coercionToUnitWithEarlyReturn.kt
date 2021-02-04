// ISSUE: KT-39075

class A {
    fun unit() {}
}

fun foo(x: () -> Unit) {}

fun main(x: A?) {

    val lambda = l@{
        if (x?.hashCode() == 0) return@l

        x?.unit()
    }

    // lambda has a type (() -> Unit?)
    <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>foo<!>(lambda)<!>
}
