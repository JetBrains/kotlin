// FILE: main.kt
package test

class MyFoo

val constructorCall = MyFoo()

fun usage(myFoo: <expr>dependency.MyFoo</expr>) {}

// FILE: dependency.kt
package dependency

class MyFoo

