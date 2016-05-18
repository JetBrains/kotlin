//ALLOW_AST_ACCESS
package test

typealias L<T> = List<T>
typealias LL<T> = L<T>
typealias LLL<T> = LL<T>

fun test1(x: L<String>) {}
fun test2(x: LL<String>) {}
fun test3(x: LLL<String>) {}
fun test4(x: L<L<String>>) {}
fun test5(x: LL<LL<String>>) {}
fun test6(x: LLL<LLL<String>>) {}
