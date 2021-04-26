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
    <error descr="[NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER] B does not refer to a type parameter of Test1">B</error> : T // error
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
class Y<<error descr="[CONFLICTING_UPPER_BOUNDS] Upper bounds of T have empty intersection">T</error>> where T :  Foo, T : <error descr="[ONLY_ONE_CLASS_BOUND_ALLOWED] Only one of the upper bounds can be a class">Bar<Foo></error>

fun <T> test2(t : T)
  where
    T : A,
    T : B,
    <error descr="[NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER] B does not refer to a type parameter of test2">B</error> : T
{
  <error descr="[TYPE_PARAMETER_ON_LHS_OF_DOT] Type parameter 'T' cannot have or inherit a companion object, so it cannot be on the left hand side of dot">T</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
  <error descr="[TYPE_PARAMETER_ON_LHS_OF_DOT] Type parameter 'T' cannot have or inherit a companion object, so it cannot be on the left hand side of dot">T</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  t.foo()
  t.bar()
}

val t1 = test2<A>(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is Jet87/A but T was expected">A()</error>)
val t2 = test2<B>(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is Jet87/C but T was expected">C()</error>)
val t3 = test2<C>(C())

val <T, B: T> Pair<T, B>.x : Int get() = 0

class Pair<A, B>()
