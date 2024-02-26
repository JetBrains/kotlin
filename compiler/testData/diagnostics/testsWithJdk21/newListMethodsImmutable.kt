// ISSUE: KT-64640, KT-65441
// WITH_STDLIB

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeLast<!>()
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!DEPRECATION_ERROR!>first<!>
    x.<!DEPRECATION_ERROR!>last<!>
}

// Additional test for other SequenceCollection inheritor
fun baz(x: ArrayDeque<String>, y: LinkedHashSet<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!DEPRECATION_ERROR!>first<!>
    x.<!DEPRECATION_ERROR!>last<!>

    y.addFirst("")
    y.addLast("")
    y.removeFirst()
    y.removeLast()
    y.getFirst()
    y.getLast()
    y.first
    y.last
}
