// FIR_IGNORE
class A {
    fun foo() {}
//      Int   Int
//      │     │
    val bar = 0
}

fun A.qux() {}

fun baz() {}

//  reflect/KFunction0<Unit>
//  │       constructor A()
//  │       │    fun (A).foo(): Unit
//  │       │    │
val test1 = A()::foo

//  reflect/KProperty0<Int>
//  │       constructor A()
//  │       │    val (A).bar: Int
//  │       │    │
val test2 = A()::bar

//  reflect/KFunction0<Unit>
//  │       constructor A()
//  │       │    fun A.qux(): Unit
//  │       │    │
val test3 = A()::qux

//  reflect/KFunction1<A, Unit>
//  │       class A
//  │       │  fun (A).foo(): Unit
//  │       │  │
val test4 = A::foo

//  reflect/KProperty1<A, Int>
//  │       class A
//  │       │  val (A).bar: Int
//  │       │  │
val test5 = A::bar

//  reflect/KFunction1<A, Unit>
//  │       class A
//  │       │  fun A.qux(): Unit
//  │       │  │
val test6 = A::qux

//  reflect/KFunction0<Unit>
//  │         fun baz(): Unit
//  │         │
val test7 = ::baz

//  reflect/KFunction1<A?, Unit>
//  │       class A
//  │       │   fun (A).foo(): Unit
//  │       │   │
val test8 = A?::foo
