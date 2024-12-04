// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57708
// RENDER_DIAGNOSTICS_FULL_TEXT

fun bar(list: List<Int>) {
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>list<!>)
}

fun <T : CharSequence> foo(list: List<T>) {}
