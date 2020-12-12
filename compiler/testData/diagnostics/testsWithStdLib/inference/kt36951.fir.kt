// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Base<T : T> : HashSet<T>() {
    fun foo() {
        super.<!INAPPLICABLE_CANDIDATE!>remove<!>("")
    }
}
