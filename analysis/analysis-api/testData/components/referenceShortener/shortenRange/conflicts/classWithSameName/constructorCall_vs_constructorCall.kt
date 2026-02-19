// FILE: main.kt
package test

class MyFoo

fun constructorCall = MyFoo()

val myDepFoo = <expr>dependency.MyFoo()</expr>

// FILE: dependency.kt
package dependency

class MyFoo

