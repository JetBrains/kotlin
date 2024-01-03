// ISSUE: KT-64702
class Box<T : CharSequence>(var value: T)

fun test(box: Box<in String>) {
    box.value.<!UNRESOLVED_REFERENCE!>length<!>
}
