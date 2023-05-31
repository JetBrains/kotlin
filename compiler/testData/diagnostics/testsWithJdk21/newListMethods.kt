// FIR_IDENTICAL
// ISSUE: KT-58371
// WITH_STDLIB

class A<T> : ArrayList<T>() {
    override fun addFirst(t: T) {
        super.addFirst(t)
    }

    override fun addLast(t: T) {
        super.addLast(t)
    }

    override fun getFirst(): T = super.getFirst()
    override fun getLast(): T = super.getLast()

    override fun removeFirst(): T = super.removeFirst()
    override fun removeLast(): T = super.removeLast()

    override fun reversed(): List<T> = super.reversed()
}

fun foo(x: MutableList<String>, y: ArrayList<String>, z: A<String>) {
    x.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    x.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.removeFirst; typeCall: extension function")!>removeFirst()<!>
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.removeLast; typeCall: extension function")!>removeLast()<!>
    x.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    y.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    y.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    y.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    y.<!UNRESOLVED_REFERENCE!>getLast<!>()
    y.<!DEBUG_INFO_CALL("fqName: kotlin.collections.removeFirst; typeCall: extension function")!>removeFirst()<!>
    y.<!DEBUG_INFO_CALL("fqName: kotlin.collections.removeLast; typeCall: extension function")!>removeLast()<!>
    y.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>

    z.<!UNRESOLVED_REFERENCE!>addFirst<!>("")
    z.<!UNRESOLVED_REFERENCE!>addLast<!>("")
    z.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    z.<!UNRESOLVED_REFERENCE!>getLast<!>()
    z.<!DEBUG_INFO_CALL("fqName: kotlin.collections.removeFirst; typeCall: extension function")!>removeFirst()<!>
    z.<!DEBUG_INFO_CALL("fqName: kotlin.collections.removeLast; typeCall: extension function")!>removeLast()<!>
    z.<!DEBUG_INFO_CALL("fqName: kotlin.collections.reversed; typeCall: extension function")!>reversed()<!>
}
