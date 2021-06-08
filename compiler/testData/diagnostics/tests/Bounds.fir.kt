// FILE: a.kt
package boundsWithSubstitutors
    open class A<T>
    class B<X : A<X>>()

    class C : A<C>()

    val a = B<C>()
    val a1 = B<<!UPPER_BOUND_VIOLATED!>Int<!>>()

    class X<A, B : A>()

    val b = X<Any, X<A<C>, C>>()
    val b0 = X<Any, <!UPPER_BOUND_VIOLATED!>Any?<!>>()
    val b1 = X<Any, X<A<C>, <!UPPER_BOUND_VIOLATED!>String<!>>>()

// FILE: b.kt
  open class A {}
  open class B<T : A>()

  class Pair<A, B>

  abstract class C<T : B<Int>, X :  (B<Char>) -> Pair<B<Any>, B<A>>>() : <!UPPER_BOUND_VIOLATED!>B<Any><!>() { // 2 errors
    val a = B<<!UPPER_BOUND_VIOLATED!>Char<!>>() // error

    abstract val x :  (B<Char>) -> B<Any>
  }


fun test() {
    foo<<!UPPER_BOUND_VIOLATED!>Int?<!>>()
    foo<Int>()
    bar<Int?>()
    bar<Int>()
    bar<<!UPPER_BOUND_VIOLATED!>Double?<!>>()
    bar<<!UPPER_BOUND_VIOLATED!>Double<!>>()
    1.<!INAPPLICABLE_CANDIDATE!>buzz<!><Double>()
}

fun <T : Any> foo() {}
fun <T : Int?> bar() {}
fun <T : <!FINAL_UPPER_BOUND!>Int<!>> Int.buzz() : Unit {}
