// FIR_IDENTICAL
open class C1
open class C2
open class C3 : C2()

class A1<T> where T : C1, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!>
class A2<T> where T : C1, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!>, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C3<!>
class A3<T> where T : C2, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C3<!>
class A4<T> where T : C3, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!>

fun <T> f1() where T : C1, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!>, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C3<!> {}
fun <T> f2() where T : C2, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C3<!> {}
fun <T> f3() where T : C3, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!> {}

enum class E1
class A5<T> where T : C1, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>E1<!>

object O1
class A6<<!CONFLICTING_UPPER_BOUNDS!>T<!>> where T : <!FINAL_UPPER_BOUND!>O1<!>, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!>

typealias TA1 = C1
typealias TA2 = C2

class A7<T> where T : C1, T : <!REPEATED_BOUND!>TA1<!>
class A8<T> where T : C1, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>TA2<!>