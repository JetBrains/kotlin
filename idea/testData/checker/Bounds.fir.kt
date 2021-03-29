  open class A {}
  open class B<T : A>()

  class Pair<A, B>

  abstract class C<T : B<Int>, X : (B<Char>) -> Pair<B<Any>, B<A>>>() : B<Any>() { // 2 errors
    val a = B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol@1f33f4d4'">Char</error>>() // error

    abstract val x : (B<Char>) -> B<Any>
  }
