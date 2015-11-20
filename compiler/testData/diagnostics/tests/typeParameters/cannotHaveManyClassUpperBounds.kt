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
class A5<<!CONFLICTING_UPPER_BOUNDS!>T<!>> where T : C1, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED, FINAL_UPPER_BOUND!>E1<!>

object O1
class A6<<!CONFLICTING_UPPER_BOUNDS!>T<!>> where T : <!FINAL_UPPER_BOUND!>O1<!>, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>C2<!>
