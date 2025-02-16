// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(list: List<String>) {
    bar(<!ARGUMENT_TYPE_MISMATCH("Array<String>; String")!>list.toTypedArray()<!>)
}

fun bar(vararg args: String) {}