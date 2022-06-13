class A<X> {
   fun <Y> foo(x: X, y: Y): Map<X, Y>

   val map: Map<X, String>
}

fun foo(a: A<Int>) {
   println(<expr>a</expr>)
}