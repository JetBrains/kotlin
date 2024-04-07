// ISSUE: KT-66638

class Super<T1, T2>
data class Child<T : Super<String, Child<T>>>(val foo: String)

fun foo(child: Child<*>) {
    child.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
}
