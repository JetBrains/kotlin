val a: Int by <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }

    fun propertyDelegated() {}

    fun propertyDelegated(a: Int) {
        a.equals(a)
    }

    fun propertyDelegated(a: String) {
        a.equals(a)
    }

    fun propertyDelegated(p: PropertyMetadata, a: Int) {
        p.equals(a)
    }
}
