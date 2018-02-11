// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNREACHABLE_CODE
//KT-2838 Type inference failed on passing null as a nullable argument
package a

fun <T> foo(a: T, b: Map<T, String>?) = b?.get(a)
fun <T> bar(a: T, b: Map<T, String>) = b.get(a)

fun test(a: Int) {
    foo(a, null)
    <!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>bar<!>(a, <!NULL_FOR_NONNULL_TYPE!>null<!>)
}
fun test1(a: Int) {
    foo(a, throw Exception())
}

fun test2(a: Int) {
    bar(a, throw Exception())
}