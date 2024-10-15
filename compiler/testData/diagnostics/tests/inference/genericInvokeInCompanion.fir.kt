// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71210
// RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

class C<T> {
    companion object {
        operator fun <T> invoke(name: String) = C<T>()
    }
}

fun main() {
    C.Companion.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>invoke<!>("")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>C<Int>.Companion<!>.<!UNRESOLVED_REFERENCE!>invoke<!>("")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>C<Int, Int, Int>.Companion<!>.<!UNRESOLVED_REFERENCE!>invoke<!>("")

    C.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>invoke<!>("")
    C<Int>.<!UNRESOLVED_REFERENCE!>invoke<!>("")
    C<Int, Int, Int>.<!UNRESOLVED_REFERENCE!>invoke<!>("")

    C.Companion.invoke<Int>("")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>C<Int>.Companion<!>.<!UNRESOLVED_REFERENCE!>invoke<!><Int>("")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>C<Int, Int, Int>.Companion<!>.<!UNRESOLVED_REFERENCE!>invoke<!><Int>("")

    C.invoke<Int>("")
    C<Int>.<!UNRESOLVED_REFERENCE!>invoke<!><Int>("")
    C<Int, Int, Int>.<!UNRESOLVED_REFERENCE!>invoke<!><Int>("")

    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>C<!>("")
    C<Int>("")
    C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>("")

    C.Companion.invoke<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>("")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>C<Int>.Companion<!>.<!UNRESOLVED_REFERENCE!>invoke<!><Int, Int, Int>("")
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>C<Int, Int, Int>.Companion<!>.<!UNRESOLVED_REFERENCE!>invoke<!><Int, Int>("")

    C.invoke<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>("")
    C<Int>.<!UNRESOLVED_REFERENCE!>invoke<!><Int, Int, Int>("")
    C<Int, Int, Int>.<!UNRESOLVED_REFERENCE!>invoke<!><Int, Int>("")
}
