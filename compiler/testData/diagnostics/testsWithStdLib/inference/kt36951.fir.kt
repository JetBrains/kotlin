// JAVAC_EXPECTED_FILE
// DIAGNOSTICS: -UNUSED_PARAMETER

class Base<T : <!CYCLIC_GENERIC_UPPER_BOUND!>T<!>> : HashSet<T>() {
    fun foo() {
        super.remove(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    }
}
