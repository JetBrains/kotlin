// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

fun testIt(l: List<Int>) {
    l.<!CANNOT_INFER_PARAMETER_TYPE!>flatMap<!> {
        f -> <!ARGUMENT_TYPE_MISMATCH!>{}<!>
    }
}
