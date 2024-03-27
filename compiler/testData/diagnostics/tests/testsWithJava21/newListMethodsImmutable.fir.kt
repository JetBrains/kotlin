// ISSUE: KT-64640, KT-65441

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeLast<!>()
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!FUNCTION_CALL_EXPECTED!>first<!>
    x.<!FUNCTION_CALL_EXPECTED!>last<!>
}

// Additional test for other SequenceCollection inheritor
fun baz(x: ArrayDeque<String>, y: LinkedHashSet<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!FUNCTION_CALL_EXPECTED!>first<!>
    x.<!FUNCTION_CALL_EXPECTED!>last<!>

    y.addFirst("")
    y.addLast("")
    y.removeFirst()
    y.removeLast()
    y.getFirst()
    y.getLast()
    y.first
    y.last
}

// Test for collections with (add/remove)(First/Last) methods which are not covered by autotests
fun foo(x: java.util.SequencedCollection<String>, y: java.util.SequencedSet<String>, z: java.util.Deque<String>) {
    x.addFirst("")
    x.addLast("")
    x.removeFirst()
    x.removeLast()
    x.getFirst()
    x.getLast()
    x.first
    x.last

    y.addFirst("")
    y.addLast("")
    y.removeFirst()
    y.removeLast()
    y.getFirst()
    y.getLast()
    y.first
    y.last

    z.addFirst("")
    z.addLast("")
    z.removeFirst()
    z.removeLast()
    z.getFirst()
    z.getLast()
    z.first
    z.last
}
