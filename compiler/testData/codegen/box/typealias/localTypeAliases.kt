// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-79185
// LANGUAGE: +NestedTypeAliases

open class Generic<K>(val k: K) {
    companion object C {
        const val prop: Int = 123
    }

    inner class Inner<K2>(val k2: K2)
}

fun box(): String {
    class Local2<T>(val p: T)

    open class Local {
        val p: String
            get() = "OK"

        <!UNSUPPORTED!>typealias LocalTAtoLocal = Local<!>
        <!UNSUPPORTED!>typealias LocaTAtoLocal2<K> = Local2<K><!>
        <!UNSUPPORTED!>typealias LocalTA2toLocal2 = Local2<String><!>

        fun test(): String {
            if (LocalTAtoLocal().p != "OK") return "FAIL"
            if (LocaTAtoLocal2("OK").p != "OK") return "FAIL"
            if (LocalTA2toLocal2("OK").p != "OK") return "FAIL"
            return "OK"
        }
    }

    <!UNSUPPORTED!>typealias TAtoLocal = Local<!>
    <!UNSUPPORTED!>typealias TAtoLocal2<K> = Local2<K><!>
    <!UNSUPPORTED!>typealias TA2toLocal2 = Local2<String><!>

    <!UNSUPPORTED!>typealias TAtoGeneric<K> = Generic<K><!>
    <!UNSUPPORTED!>typealias TAtoInner<K, L> = Generic<K>.Inner<L><!>

    if (<!UNRESOLVED_REFERENCE!>TAtoLocal<!>().p != "OK") return "FAIL"
    if (<!UNRESOLVED_REFERENCE!>TAtoLocal2<!>("OK").p != "OK") return "FAIL"
    if (<!UNRESOLVED_REFERENCE!>TA2toLocal2<!>("OK").p != "OK") return "FAIL"

    val callable = ::<!UNRESOLVED_REFERENCE!>TAtoLocal<!>
    if (<!UNRESOLVED_REFERENCE!>callable<!>().p != "OK") return "FAIL"

    fun localFunc(): <!UNRESOLVED_REFERENCE!>TAtoLocal<!> = <!UNRESOLVED_REFERENCE!>TAtoLocal<!>()
    if (localFunc().<!UNRESOLVED_REFERENCE!>test<!>() != "OK") return "FAIL"

    if (<!UNRESOLVED_REFERENCE!>TAtoGeneric<!><String>("OK").k != "OK") return "FAIL"
    val genericObj = Generic<Int>(42)
    if (genericObj.<!UNRESOLVED_REFERENCE!>TAtoInner<!><Int, String>("OK").k2 != "OK") return "FAIL"

    val anonObject = object : <!UNRESOLVED_REFERENCE!>TAtoLocal<!>() {}
    if (anonObject.<!UNRESOLVED_REFERENCE!>test<!>() != "OK") return "FAIL"

    val anonObject2 = object : <!UNRESOLVED_REFERENCE!>TAtoGeneric<!><String>("OK") {}
    if (anonObject2.<!UNRESOLVED_REFERENCE!>k<!> != "OK") return "FAIL"

    val companion = <!UNRESOLVED_REFERENCE!>TAtoGeneric<!>
    if (companion.prop != 123) return "FAIL"

    val typeRefToLocalTypeAliasInLocalClass: Local.<!UNRESOLVED_REFERENCE!>LocalTAtoLocal<!> = Local.LocalTAtoLocal()
    if (typeRefToLocalTypeAliasInLocalClass.<!UNRESOLVED_REFERENCE!>p<!> != "OK") return "FAIL"

    return "OK"
}
