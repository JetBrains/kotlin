// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(list: List<String>) {
    bar(<!TYPE_MISMATCH("String; Array<String>")!>list.toTypedArray()<!>)
}

fun bar(vararg args: String) {}