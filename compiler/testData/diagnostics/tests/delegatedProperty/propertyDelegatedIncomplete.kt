// !DIAGNOSTICS: -UNUSED_PARAMETER

val a: Int by  <!DELEGATE_PD_METHOD_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    fun <T> propertyDelegated(p: PropertyMetadata) {}
}
