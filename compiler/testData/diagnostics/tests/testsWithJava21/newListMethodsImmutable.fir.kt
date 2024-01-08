// ISSUE: KT-64640

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>removeLast<!>()
}

// Additional test for other SequenceCollection inheritor
fun baz(x: ArrayDeque<String>, y: LinkedHashSet<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()

    y.addFirst("")
    y.addLast("")
    y.removeFirst()
    y.removeLast()
}
