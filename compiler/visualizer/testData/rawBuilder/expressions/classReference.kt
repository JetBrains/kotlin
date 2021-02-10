// FIR_IGNORE
//WITH_RUNTIME
package test

class A

fun test() {
//  class A
//  │
    A::class
    test.A::class
//  constructor A()
//  │
    A()::class

//  class A  val <T> reflect/KClass<A>.java: java/lang/Class<A>
//  │        │
    A::class.java
//                val <T> reflect/KClass<A>.java: java/lang/Class<A>
//                │
    test.A::class.java
//  constructor A()
//  │          val <T> reflect/KClass<out A>.java: java/lang/Class<out A>
//  │          │
    A()::class.java
}
