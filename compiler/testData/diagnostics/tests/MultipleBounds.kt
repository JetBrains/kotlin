package Jet87

open class A() {
  fun foo() : Int = 1
}

trait B {
  fun bar() : Double = 1.0;
}

class C() : A(), B

class D() {
  class object : A(), B {}
}

class Test1<T : A>()
  where
    T : B,
    <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T, // error
    class object T : A,
    class object T : B,
    class object <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T
  {

  fun test(t : T) {
    T.foo()
    T.bar()
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
    <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T,
    class object <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>B<!> : T,
    class object T : B,
    class object T : A
{
  T.foo()
  T.bar()
  t.foo()
  t.bar()
}

val t1 = test2<<!UPPER_BOUND_VIOLATED!>A<!>>(A())
val t2 = test2<<!UPPER_BOUND_VIOLATED!>B<!>>(C())
val t3 = test2<C>(C())

class Test<<!CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS!>T<!>>
  where
    class object T : <!FINAL_CLASS_OBJECT_UPPER_BOUND!>Foo<!>,
    class object T : A {}

val <T, B : T> x : Int = 0
