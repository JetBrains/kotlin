fun foo(ll: java.util.LinkedList<String>, al: ArrayList<String>, ad: ArrayDeque<String>, jad: java.util.ArrayDeque<String>) {
    ll.addFirst("")
    ll.addLast("")
    ll.<!DEPRECATION!>getFirst<!>()
    ll.<!DEPRECATION!>first<!> // synthetic property for getFirst()
    ll.first() // stdlib extension on List
    ll.<!DEPRECATION!>getLast<!>()
    ll.<!DEPRECATION!>last<!>
    ll.last()
    ll.removeFirst()
    ll.removeLast()
    ll.reversed()

    al.addFirst("")
    al.addLast("")
    al.<!DEPRECATION!>getFirst<!>()
    al.<!DEPRECATION!>first<!>
    al.first()
    al.<!DEPRECATION!>getLast<!>()
    al.<!DEPRECATION!>last<!>
    al.last()
    al.removeFirst()
    al.removeLast()
    al.reversed()

    ad.addFirst("")
    ad.addLast("")
    ad.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    ad.<!FUNCTION_CALL_EXPECTED!>first<!>
    ad.first()
    ad.<!UNRESOLVED_REFERENCE!>getLast<!>()
    ad.<!FUNCTION_CALL_EXPECTED!>last<!>
    ad.last()
    ad.removeFirst()
    ad.removeLast()
    ad.reversed()

    jad.addFirst("")
    jad.addLast("")
    jad.getFirst()
    jad.first
    jad.first()
    jad.getLast()
    jad.last
    jad.last()
    jad.removeFirst()
    jad.removeLast()
    jad.reversed()
}
