trait Tr

class A { default object }
class B { default object : Tr }

fun Any.f1() {}
fun Any?.f2() {}
fun Tr.f3() {}
fun Tr?.f4() {}
fun A.f5() {}

fun test() {
    A.f1()
    A.f2()
    B.f3()
    B.f4()
    A.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f5<!>()
    B.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f5<!>()
}