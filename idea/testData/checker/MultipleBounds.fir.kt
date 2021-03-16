package Jet87

open class A() {
  fun foo() : Int = 1
}

interface B {
  fun bar() : Double = 1.0;
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
    T.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    T.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    t.foo()
    t.bar()
  }
}

fun test() {
  Test1<B>()
  Test1<A>()
  Test1<C>()
}

class Foo() {}

class Bar<T : Foo>

class Buzz<T> where T : Bar<Int>, T : <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: nioho">nioho</error>

class X<T : Foo>
class Y<T> where T :  Foo, T : Bar<Foo>

fun <T> test2(t : T)
  where
    T : A,
    T : B,
    B : T
{
  T.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
  T.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  t.foo()
  t.bar()
}

val t1 = <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): Jet87/test2">test2</error><A>(A())
val t2 = <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): Jet87/test2">test2</error><B>(C())
val t3 = test2<C>(C())

val <T, B: T> Pair<T, B>.x : Int get() = 0

class Pair<A, B>()
