  open class A {}
  open class B<T : A>()

  class Pair<A, B>

  abstract class C<T : B<Int>, X : (B<Char>) -> Pair<B<Any>, B<A>>>() : B<Any>() { // 2 errors
    val a = B<Char>() // error

    abstract val x : (B<Char>) -> B<Any>
  }
