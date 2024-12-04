// FILE: main.kt
package test

class MyFoo

val constructorCall = MyFoo()

val nested = <expr>dependency.MyFoo</expr>.Nested()

// FILE: dependency.kt
package dependency

class MyFoo {
    class Nested
}

