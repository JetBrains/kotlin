class Outer<O> {
   inner class A<X> {
      fun <Y> foo(x: X, y: Y): Map<X, Map<Y, O>>

      val map: Map<X, O>
   }
}

fun foo(a: Outer<String>.A<Int>) {
   println(<expr>a</expr>)
}
