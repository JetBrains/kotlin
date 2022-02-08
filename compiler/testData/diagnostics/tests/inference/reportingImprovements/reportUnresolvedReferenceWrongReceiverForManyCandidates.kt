fun test() {
    val a = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>-<!>false
}

operator fun A.unaryMinus() {}
operator fun B.unaryMinus() {}
class A
class B
