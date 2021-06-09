// EXPECTED_DUPLICATED_HIGHLIGHTING
  open class A {}
  open class B<T : A>()

  class Pair<A, B>

  abstract class C<T : B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Int</error>>, X : (B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'"><error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Char</error></error>>) -> Pair<B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Any</error>>, B<A>>>() : B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Any</error>>() { // 2 errors
    val a = B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Char</error>>() // error

    abstract val x : (B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'"><error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Char</error></error>>) -> B<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'A'">Any</error>>
  }

