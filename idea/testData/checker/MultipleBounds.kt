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
    <error>B</error> : T // error
  {

  fun test(t : T) {
    <error>T</error>.<error>foo</error>()
    <error>T</error>.<error>bar</error>()
    t.foo()
    t.bar()
  }
}

fun test() {
  Test1<<error>B</error>>()
  Test1<<error>A</error>>()
  Test1<C>()
}

class Foo() {}

class Bar<T : <warning>Foo</warning>>

class Buzz<T> where T : <warning>Bar<<error>Int</error>></warning>, T : <error>nioho</error>

class X<T : <warning>Foo</warning>>
class Y<<error>T</error>> where T :  <warning>Foo</warning>, T : <error>Bar<Foo></error>

fun <T> test2(t : T)
  where
    T : A,
    T : B,
    <error>B</error> : T
{
  <error>T</error>.<error>foo</error>()
  <error>T</error>.<error>bar</error>()
  t.foo()
  t.bar()
}

val t1 = test2<<error>A</error>>(A())
val t2 = test2<<error>B</error>>(C())
val t3 = test2<C>(C())

val <T, B: T> Pair<T, B>.x : Int get() = 0

class Pair<A, B>()
