// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    private fun propertyDelegated(p: PropertyMetadata) {}
}
