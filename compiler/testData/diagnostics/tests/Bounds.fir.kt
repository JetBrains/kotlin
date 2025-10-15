// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt
package boundsWithSubstitutors
    open class A<T>
    class B<X : A<X>>()

    class C : A<C>()

    val a = B<C>()
    val a1 = B<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Int<!>>()

    class X<A, B : A>()

    val b = X<Any, X<A<C>, C>>()
    val b0 = X<Any, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Any?<!>>()
    val b1 = X<Any, X<A<C>, <!UPPER_BOUND_VIOLATED!>String<!>>>()

// FILE: b.kt
  open class A {}
  open class B<T : A>()

  class Pair<A, B>

  abstract class C<T : B<<!UPPER_BOUND_VIOLATED!>Int<!>>, X :  (B<<!UPPER_BOUND_VIOLATED!>Char<!>>) -> Pair<B<<!UPPER_BOUND_VIOLATED!>Any<!>>, B<A>>>() : B<<!UPPER_BOUND_VIOLATED!>Any<!>>() { // 2 errors
    val a = B<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Char<!>>() // error

    abstract val x :  (B<<!UPPER_BOUND_VIOLATED!>Char<!>>) -> B<<!UPPER_BOUND_VIOLATED!>Any<!>>
  }


fun test() {
    foo<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Int?<!>>()
    foo<Int>()
    bar<Int?>()
    bar<Int>()
    bar<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Double?<!>>()
    bar<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>Double<!>>()
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>buzz<!><Double>()
}

fun <T : Any> foo() {}
fun <T : Int?> bar() {}
fun <T : <!FINAL_UPPER_BOUND!>Int<!>> Int.buzz() : Unit {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
nullableType, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
