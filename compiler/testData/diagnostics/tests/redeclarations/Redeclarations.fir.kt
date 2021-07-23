// FILE: a.kt
package redeclarations
  object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    val x : Int = 0

    val A = 1
  }

  class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {}

  val <!REDECLARATION!>A<!> = 1

// FILE: b.kt
  package redeclarations.A
    class A {}
