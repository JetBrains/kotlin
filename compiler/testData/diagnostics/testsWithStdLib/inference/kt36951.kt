// !JAVAC_EXPECTED_FILE
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Base<T : <!CYCLIC_GENERIC_UPPER_BOUND!>T<!>> : HashSet<T>() {
    fun foo() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.<!TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING!>remove<!>("")
    }
}
