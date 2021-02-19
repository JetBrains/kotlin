// FIR_IGNORE
//WITH_RUNTIME
package test

class A

fun test() {
//  class A
//  │
    A::class
//  class A
//  │
    test.A::class
//  constructor A()
//  │
    A()::class

//  class A  val <T> reflect/KClass<T>.java: java/lang/Class<T>
//  │        │
    A::class.java
//  class A       val <T> reflect/KClass<T>.java: java/lang/Class<T>
//  │             │
    test.A::class.java
//  constructor A()
//  │          val <T> reflect/KClass<T>.java: java/lang/Class<T>
//  │          │
    A()::class.java
}
