// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(list: List<String>) {
    bar(<!ARGUMENT_TYPE_MISMATCH("Array<String>; String;  value parameter 'args' of;  'list.toTypedArray()'")!>list.toTypedArray()<!>)
}

fun bar(vararg args: String) {}

/* GENERATED_FIR_TAGS: functionDeclaration, vararg */
