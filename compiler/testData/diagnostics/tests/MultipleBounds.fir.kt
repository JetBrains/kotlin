package Jet87

open class A() {
  fun foo() : Int = 1
}

interface B {
  fun bar() : Double = 1.0;
}

interface G<X> {
    val <X> boo: Double  where X : A, X : B
    val <A> bal: Double  where A : B
    val <Y> bas: Double where Y : B, X : B
}

class C() : A(), B

class D() {
  companion object : A(), B {}
}

class Test1<T>()
  where
    T : A,
    T : B,
    B : T // error
  {

  fun test(t : T) {
    <!OTHER_ERROR!>T<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!OTHER_ERROR!>T<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
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

class Bar<T : Foo>

class Buzz<T> where T : Bar<Int>, T : <!UNRESOLVED_REFERENCE!>nioho<!>

class X<T : Foo>
class Y<T> where T : Foo, T : Bar<Foo>

fun <T> test2(t : T)
  where
    T : A,
    T : B,
    B : T
{
  <!OTHER_ERROR!>T<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
  <!OTHER_ERROR!>T<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
  t.foo()
  t.bar()
}

val t1 = <!INAPPLICABLE_CANDIDATE!>test2<!><A>(A())
val t2 = <!INAPPLICABLE_CANDIDATE!>test2<!><B>(C())
val t3 = test2<C>(C())

val <T, B : T> x : Int = 0