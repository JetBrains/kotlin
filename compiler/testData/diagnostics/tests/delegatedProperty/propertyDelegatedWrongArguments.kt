// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    fun propertyDelegated() {}

    fun propertyDelegated(a: Int) {}

    fun propertyDelegated(a: String) {}

    fun propertyDelegated(p: PropertyMetadata, a: Int) {}
}
