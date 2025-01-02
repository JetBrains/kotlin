// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(list: List<String>) {
    bar(<!ARGUMENT_TYPE_MISMATCH("kotlin.String; kotlin.Array<T (of fun <T> Collection<T>.toTypedArray)>")!>list.toTypedArray()<!>)
}

fun bar(vararg args: String) {}