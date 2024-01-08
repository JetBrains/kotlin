fun foo(ll: java.util.LinkedList<String>, al: ArrayList<String>, ad: ArrayDeque<String>, jad: java.util.ArrayDeque<String>) {
    ll.addFirst("")
    ll.addLast("")
    ll.<!DEPRECATION!>getFirst<!>()
    ll.<!DEPRECATION!>first<!> // synthetic property for getFirst()
    ll.first() // stdlib extension on List
    ll.<!DEPRECATION!>getLast<!>()
    ll.<!DEPRECATION!>last<!>
    ll.last()
    ll.<!DEBUG_INFO_CALL("fqName: java.util.LinkedList.removeFirst; typeCall: function")!>removeFirst()<!>
    ll.<!DEBUG_INFO_CALL("fqName: java.util.LinkedList.removeLast; typeCall: function")!>removeLast()<!>
    ll.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    al.addFirst("")
    al.addLast("")
    al.<!DEPRECATION!>getFirst<!>()
    al.<!DEPRECATION!>first<!>
    al.first()
    al.<!DEPRECATION!>getLast<!>()
    al.<!DEPRECATION!>last<!>
    al.last()
    al.<!DEBUG_INFO_CALL("fqName: java.util.ArrayList.removeFirst; typeCall: function")!>removeFirst()<!>
    al.<!DEBUG_INFO_CALL("fqName: java.util.ArrayList.removeLast; typeCall: function")!>removeLast()<!>
    al.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    ad.addFirst("")
    ad.addLast("")
    ad.<!DEPRECATION!>getFirst<!>()
    ad.<!DEPRECATION!>first<!>
    ad.first()
    ad.<!DEPRECATION!>getLast<!>()
    ad.<!DEPRECATION!>last<!>
    ad.last()
    ad.<!DEBUG_INFO_CALL("fqName: kotlin.collections.ArrayDeque.removeFirst; typeCall: function")!>removeFirst()<!>
    ad.<!DEBUG_INFO_CALL("fqName: kotlin.collections.ArrayDeque.removeLast; typeCall: function")!>removeLast()<!>
    ad.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    jad.addFirst("")
    jad.addLast("")
    jad.getFirst()
    jad.first
    jad.first()
    jad.getLast()
    jad.last
    jad.last()
    jad.<!DEBUG_INFO_CALL("fqName: java.util.ArrayDeque.removeFirst; typeCall: function")!>removeFirst()<!>
    jad.<!DEBUG_INFO_CALL("fqName: java.util.ArrayDeque.removeLast; typeCall: function")!>removeLast()<!>
    jad.<!DEBUG_INFO_CALL("fqName: java.util.ArrayDeque.reversed; typeCall: function")!>reversed()<!>
}