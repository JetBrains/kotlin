class A {
    val c: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

class Delegate {
    fun getValue(t: Int, p: PropertyMetadata): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }

    fun getValue(t: String, p: PropertyMetadata): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }
}
