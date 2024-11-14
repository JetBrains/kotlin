// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

fun foo(x: MutableMap<String, List<String>>) {
    x.merge("", listOf("")) { a, b -> a + b }
}
