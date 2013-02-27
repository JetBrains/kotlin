//KT-2838 Type inference failed on passing null as a nullable argument
package a

fun foo<T>(a: T, b: Map<T, String>?) = b?.get(a)
fun bar<T>(a: T, b: Map<T, String>) = b.get(a)

fun test(a: Int) {
    foo(a, null)
    bar(a, <!ERROR_COMPILE_TIME_VALUE!>null<!>)
}
fun test1(a: Int) {
    <!UNREACHABLE_CODE!>foo(a, throw Exception())<!>
}

fun test2(a: Int) {
    <!UNREACHABLE_CODE!>bar(a, throw Exception())<!>
}