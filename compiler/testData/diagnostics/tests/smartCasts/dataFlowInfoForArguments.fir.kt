// RUN_PIPELINE_TILL: BACKEND
package aaa

fun bar(a: Int, b: Int) {}

fun foo(a: Int?) {
    bar(a!!, a)
}
