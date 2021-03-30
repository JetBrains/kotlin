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
    <error descr="[TYPE_PARAMETER_ON_LHS_OF_DOT] Type parameter 'T' cannot have or inherit a companion object, so it cannot be on the left hand side of dot">T</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    <error descr="[TYPE_PARAMETER_ON_LHS_OF_DOT] Type parameter 'T' cannot have or inherit a companion object, so it cannot be on the left hand side of dot">T</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    t.foo()
    t.bar()
  }
}

fun test() {
  Test1<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'it(Jet87/A & Jet87/B)'">B</error>>()
  Test1<<error descr="[UPPER_BOUND_VIOLATED] Type argument is not within its bounds: should be subtype of 'it(Jet87/A & Jet87/B)'">A</error>>()
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
  <error descr="[TYPE_PARAMETER_ON_LHS_OF_DOT] Type parameter 'T' cannot have or inherit a companion object, so it cannot be on the left hand side of dot">T</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
  <error descr="[TYPE_PARAMETER_ON_LHS_OF_DOT] Type parameter 'T' cannot have or inherit a companion object, so it cannot be on the left hand side of dot">T</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  t.foo()
  t.bar()
}

val t1 = <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): Jet87/test2">test2</error><A>(A())
val t2 = <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): Jet87/test2">test2</error><B>(C())
val t3 = test2<C>(C())

val <T, B: T> Pair<T, B>.x : Int get() = 0

class Pair<A, B>()
