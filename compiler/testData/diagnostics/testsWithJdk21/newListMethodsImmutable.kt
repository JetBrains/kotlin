// ISSUE: KT-64640, KT-65441
// WITH_STDLIB

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeLast<!>()
    x.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>getFirst<!>()
    x.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>getLast<!>()
    x.<!DEPRECATION!>first<!>
    x.<!DEPRECATION!>last<!>
}

// Additional test for other SequenceCollection inheritor
fun baz(x: ArrayDeque<String>, y: LinkedHashSet<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()
    x.<!DEPRECATION!>getFirst<!>()
    x.<!DEPRECATION!>getLast<!>()
    x.<!DEPRECATION!>first<!>
    x.<!DEPRECATION!>last<!>

    y.addFirst("")
    y.addLast("")
    y.removeFirst()
    y.removeLast()
    y.getFirst()
    y.getLast()
    y.first
    y.last
}
