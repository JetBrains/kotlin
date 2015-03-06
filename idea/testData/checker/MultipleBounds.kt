package Jet87

open class A() {
  fun foo() : Int = 1
}

trait B {
  fun bar() : Double = 1.0;
}

class C() : A(), B

class D() {
  default object : A(), B {}
}

class Test1<T : A>()
  where
    T : B,
    <error>B</error> : T, // error
    <error>class object T : A</error>,
    <error>class object T : B</error>,
    <error>class object <error>B</error> : T</error>
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
class Y<<error>T</error> : <warning>Foo</warning>> where T : <warning>Bar<Foo></warning>

fun <T : A> test2(t : T)
  where
    T : B,
    <error>B</error> : T,
    <error>class object <error>B</error> : T</error>,
    <error>class object T : B</error>,
    <error>class object T : A</error>
{
  <error>T</error>.<error>foo</error>()
  <error>T</error>.<error>bar</error>()
  t.foo()
  t.bar()
}

val t1 = test2<<error>A</error>>(A())
val t2 = test2<<error>B</error>>(C())
val t3 = test2<C>(C())

class Test<T>
  where
    <error>class object T : <error>Foo</error></error>,
    <error>class object T : A</error> {}

val <T, B : T> x : Int = 0
