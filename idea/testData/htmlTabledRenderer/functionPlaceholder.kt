class A<T, R>
fun <T, R> foo(a: A<T, R>) = a

fun test() {
    foo { it }
    foo { x -> x}
    foo { (x: Int) -> x}
}