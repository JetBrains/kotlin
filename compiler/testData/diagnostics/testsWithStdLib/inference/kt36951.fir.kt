// !JAVAC_EXPECTED_FILE
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Base<T : T> : HashSet<T>() {
    fun foo() {
        super.remove(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    }
}
