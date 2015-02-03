// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by <!DELEGATE_SPECIAL_FUNCTION_AMBIGUITY!>Delegate()<!>

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    fun propertyDelegated(p: PropertyMetadata) {}

    fun propertyDelegated(p: PropertyMetadata, s: String = "") {}
}
