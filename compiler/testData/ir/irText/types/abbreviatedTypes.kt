// FIR_IDENTICAL
typealias I = Int
typealias L<T> = List<T>

fun test1(x: L<I>) = x
fun test2(x: List<L<I>>) = x
fun test3(x: L<List<I>>) = x
fun test4(x: L<L<I>>) = x