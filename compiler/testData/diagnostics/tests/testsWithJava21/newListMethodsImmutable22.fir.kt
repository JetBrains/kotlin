// ISSUE: KT-64640, KT-65441
// LANGUAGE_VERSION: 2.2
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun bar(x: List<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE!>removeFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>removeLast<!>()
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!FUNCTION_CALL_EXPECTED!>first<!>
    x.<!FUNCTION_CALL_EXPECTED!>last<!>
}

// Additional test for other SequenceCollection inheritor
fun baz(x: ArrayDeque<String>, y: LinkedHashSet<String>, z: java.util.LinkedList<String>) {
    x.addFirst("")
    x.addLast("")
    x.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    val removed1 = x.removeFirst()
    val removed2 = x.removeLast()
    val got1 = x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    val got2 = x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    val got3 = x.<!FUNCTION_CALL_EXPECTED!>first<!>
    val got4 = x.<!FUNCTION_CALL_EXPECTED!>last<!>

    y.addFirst("")
    y.addLast("")
    y.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    y.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    var removed3 = y.removeFirst()
    removed3 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var removed4 = y.removeLast()
    removed4 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var got5 = y.getFirst()
    got5 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var got6 = y.getLast()
    got6 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var got7 = y.first
    got7 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var got8 = y.last
    got8 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    z.addFirst("")
    z.addLast("")
    z.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    z.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    var removed5 = z.removeFirst()
    removed5 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var removed6 = z.removeLast()
    removed6 = <!NULL_FOR_NONNULL_TYPE!>null<!>
    var got9 = z.<!DEPRECATION!>getFirst<!>()
    got9 = null
    var got10 = z.<!DEPRECATION!>getLast<!>()
    got10 = null
    var got11 = z.<!DEPRECATION!>first<!>
    got11 = null
    var got12 = z.<!DEPRECATION!>last<!>
    got12 = null
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
