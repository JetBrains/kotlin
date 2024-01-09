// ISSUE: KT-64640

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeLast<!>()
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

// Test for collections with (add/remove)(First/Last) methods which are not covered by autotests
fun foo(x: java.util.SequencedCollection<String>, y: java.util.SequencedSet<String>, z: java.util.Deque<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()

    y.addFirst("")
    y.addLast("")
    y.removeFirst()
    y.removeLast()

    z.addFirst("")
    z.addLast("")
    z.removeFirst()
    z.removeLast()
}
