// ISSUE: KT-58371
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT

class A<T> : ArrayList<T>() {
    override fun addFirst(t: T) {
        super.addFirst(t)
    }

    override fun addLast(t: T) {
        super.addLast(t)
    }

    override fun <!OVERRIDE_DEPRECATION!>getFirst<!>(): T = super.<!DEPRECATION!>getFirst<!>()
    override fun <!OVERRIDE_DEPRECATION!>getLast<!>(): T = super.<!DEPRECATION!>getLast<!>()

    override fun removeFirst(): T = super.removeFirst()
    override fun removeLast(): T = super.removeLast()

    override fun reversed(): List<T> = super.reversed()
}

fun foo(x: MutableList<String>, y: ArrayList<String>, z: A<String>) {
    x.addFirst("")
    x.addLast("")
    x.<!DEPRECATION!>getFirst<!>()
    x.<!DEPRECATION!>first<!> // synthetic property for getFirst()
    x.first() // stdlib extension on List
    x.<!DEPRECATION!>getLast<!>()
    x.<!DEPRECATION!>last<!>
    x.last()
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.MutableList.removeFirst; typeCall: function")!>removeFirst()<!>
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.MutableList.removeLast; typeCall: function")!>removeLast()<!>
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    y.addFirst("")
    y.addLast("")
    y.<!DEPRECATION!>getFirst<!>()
    y.<!DEPRECATION!>first<!>
    y.first()
    y.<!DEPRECATION!>getLast<!>()
    y.<!DEPRECATION!>last<!>
    y.last()
    y.<!DEBUG_INFO_CALL("fqName: java.util.ArrayList.removeFirst; typeCall: function")!>removeFirst()<!>
    y.<!DEBUG_INFO_CALL("fqName: java.util.ArrayList.removeLast; typeCall: function")!>removeLast()<!>
    y.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    z.addFirst("")
    z.addLast("")
    z.<!DEPRECATION!>getFirst<!>()
    z.<!DEPRECATION!>first<!>
    z.first()
    z.<!DEPRECATION!>getLast<!>()
    z.<!DEPRECATION!>last<!>
    z.last()
    z.<!DEBUG_INFO_CALL("fqName: A.removeFirst; typeCall: function")!>removeFirst()<!>
    z.<!DEBUG_INFO_CALL("fqName: A.removeLast; typeCall: function")!>removeLast()<!>
    z.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>
}
