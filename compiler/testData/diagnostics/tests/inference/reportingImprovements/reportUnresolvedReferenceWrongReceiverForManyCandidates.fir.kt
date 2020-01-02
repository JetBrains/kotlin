fun test() {
    val a = <!INAPPLICABLE_CANDIDATE!>-<!>false
}

operator fun A.unaryMinus() {}
operator fun B.unaryMinus() {}
class A
class B