package Jet87

open class A() {
  fun foo() : Int = 1
}

interface B {
  fun bar() : Double = 1.0;
}

interface G<X> {
    val <X : A> boo: Double  where X : B
    val <A> bal: Double  where A : B
    val <Y : B> bas: Double where <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>X<!> : B
}

class C() : A(), B

class D() {
  companion object : A(), B {}
}

class Test1<T : A>()
  where
    T : B,
    <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T // error
  {

  fun test(t : T) {
    <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
    t.foo()
    t.bar()
  }
}

fun test() {
  Test1<<!UPPER_BOUND_VIOLATED!>B<!>>()
  Test1<<!UPPER_BOUND_VIOLATED!>A<!>>()
  Test1<C>()
}

class Foo() {}

class Bar<T : <!FINAL_UPPER_BOUND!>Foo<!>>

class Buzz<T> where T : <!FINAL_UPPER_BOUND!>Bar<<!UPPER_BOUND_VIOLATED!>Int<!>><!>, T : <!UNRESOLVED_REFERENCE!>nioho<!>

class X<T : <!FINAL_UPPER_BOUND!>Foo<!>>
class Y<<!CONFLICTING_UPPER_BOUNDS!>T<!> : <!FINAL_UPPER_BOUND!>Foo<!>> where T : <!FINAL_UPPER_BOUND!>Bar<Foo><!>

fun <T : A> test2(t : T)
  where
    T : B,
    <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T
{
  <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
  <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
  t.foo()
  t.bar()
}

val t1 = test2<<!UPPER_BOUND_VIOLATED!>A<!>>(A())
val t2 = test2<<!UPPER_BOUND_VIOLATED!>B<!>>(C())
val t3 = test2<C>(C())

val <T, B : T> x : Int = 0