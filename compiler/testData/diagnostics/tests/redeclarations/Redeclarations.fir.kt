// FILE: f.kt
package redeclarations
  object A {
    val x : Int = 0

    val A = 1
  }

  class A {}

  val A = 1

// FILE: f.kt
  package redeclarations.A
    class A {}
