// RUN_PIPELINE_TILL: BACKEND
fun String.bar(s: String) = s

fun foo(s: String?) {
    s?.bar(s)
    s?.get(s.length)
}