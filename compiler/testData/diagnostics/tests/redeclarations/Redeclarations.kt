// FILE: _.kt
package redeclarations
  object <!REDECLARATION!>A<!> {
    val x : Int = 0

    val A = 1
  }

  class <!REDECLARATION!>A<!> {}

  val <!REDECLARATION!>A<!> = 1

// FILE: _.kt
  package redeclarations.<!REDECLARATION!>A<!>
    class A {}
