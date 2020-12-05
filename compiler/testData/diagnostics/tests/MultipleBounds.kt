package Jet87

open class A() {
  fun foo() : Int = 1
}

interface B {
  fun bar() : Double = 1.0;
}

interface G<X> {
    val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>X<!>> boo: Double  where X : A, X : B
    val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>A<!>> bal: Double  where A : B
    val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>Y<!>> bas: Double where Y : B, <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>X<!> : B
}

class C() : A(), B

class D() {
  companion object : A(), B {}
}

class Test1<T>()
  where
    T : A,
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
class Y<<!CONFLICTING_UPPER_BOUNDS!>T<!>> where T : <!FINAL_UPPER_BOUND!>Foo<!>, T : <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>Bar<Foo><!>

fun <T> test2(t : T)
  where
    T : A,
    T : B,
    <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T
{
  <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
  <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
  t.foo()
  t.bar()
}

val t1 = test2<<!UPPER_BOUND_VIOLATED!>A<!>>(<!TYPE_MISMATCH!>A()<!>)
val t2 = test2<<!UPPER_BOUND_VIOLATED!>B<!>>(C())
val t3 = test2<C>(C())

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>, <!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>B : T<!>> x : Int = 0