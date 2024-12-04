// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57708
// RENDER_DIAGNOSTICS_FULL_TEXT

fun bar(list: List<Int>) {
    foo(<!TYPE_MISMATCH!>list<!>)
}

fun <T : CharSequence> foo(list: List<T>) {}
