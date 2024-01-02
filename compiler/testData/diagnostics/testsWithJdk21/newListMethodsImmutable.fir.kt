// ISSUE: KT-64640
// WITH_STDLIB

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>removeLast<!>()
}
