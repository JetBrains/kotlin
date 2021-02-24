// FILE: a.kt
package redeclarations
  <!REDECLARATION!>object A<!> {
    val x : Int = 0

    val A = 1
  }

  <!REDECLARATION!>class A {}<!>

  <!REDECLARATION!>val A = 1<!>

// FILE: b.kt
  package redeclarations.A
    class A {}
