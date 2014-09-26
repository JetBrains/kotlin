val a: Int by <!DELEGATE_SPECIAL_FUNCTION_AMBIGUITY!>Delegate()<!>

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }

    fun propertyDelegated(p: PropertyMetadata) {
        p.equals(p)
    }

    fun propertyDelegated(p: PropertyMetadata, s: String = "") {
        p.equals(s)
    }
}
