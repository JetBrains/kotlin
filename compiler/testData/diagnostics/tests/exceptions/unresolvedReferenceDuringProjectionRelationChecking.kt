// FIR_IDENTICAL
// ISSUE: KT-69407

fun <T> <!UNRESOLVED_REFERENCE!>UnresolvedClass<!><out T>.test() { }

class Box<T>
typealias TA<K> = Box<K>

fun checkList(list: TA<out <!UNRESOLVED_REFERENCE!>UnresolvedClass<!>>) {}
