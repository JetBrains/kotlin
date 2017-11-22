// !WITH_NEW_INFERENCE
interface Tr

class A { companion object }
class B { companion object : Tr }

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
    A.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f5<!>()
    B.<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f5<!>()
}