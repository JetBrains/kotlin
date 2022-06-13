class Outer<O> {
   class A<X> {
      fun <Y> foo(x: X, y: Y): Map<X, Map<Y, O>>

      val map: Map<X, O>
   }
}

fun foo(a: Outer.A<Int>) {
   println(<expr>a</expr>)
}