interface A<T> {
   fun <R> foo(r: R)
}

fun call(a: A<String>) {
   a.<expr>foo(1)</expr>
}
