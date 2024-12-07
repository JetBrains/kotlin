// RUN_PIPELINE_TILL: BACKEND
class A

fun foo(s: String) {}

fun test(a: A) {
    foo("$a")
}
