// !WITH_NEW_INFERENCE
// FILE: a.kt
package boundsWithSubstitutors
    open class A<T>
    class B<X : A<X>>()

    class C : A<C>()

    val a = B<C>()
    val a1 = B<Int>()

    class X<A, B : A>()

    val b = X<Any, X<A<C>, C>>()
    val b0 = X<Any, Any?>()
    val b1 = X<Any, X<A<C>, String>>()

// FILE: b.kt
  open class A {}
  open class B<T : A>()

  class Pair<A, B>

  abstract class C<T : B<Int>, X :  (B<Char>) -> Pair<B<Any>, B<A>>>() : B<Any>() { // 2 errors
    val a = B<Char>() // error

    abstract val x :  (B<Char>) -> B<Any>
  }


fun test() {
    foo<Int?>()
    foo<Int>()
    bar<Int?>()
    bar<Int>()
    bar<Double?>()
    bar<Double>()
    1.<!INAPPLICABLE_CANDIDATE!>buzz<!><Double>()
}

fun <T : Any> foo() {}
fun <T : Int?> bar() {}
fun <T : Int> Int.buzz() : Unit {}
