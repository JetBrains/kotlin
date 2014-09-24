val a: Int by <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }

    private fun propertyDelegated(p: PropertyMetadata) {
        p.equals(p)
    }
}
