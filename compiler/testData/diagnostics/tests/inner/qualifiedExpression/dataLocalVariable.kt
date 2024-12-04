// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun bar(b: Boolean) = b

fun foo(data: List<String>) {
    bar(data.contains(""))
}
