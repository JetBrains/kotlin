// FILE: f.kt
package redeclarations
  object <!PACKAGE_OR_CLASSIFIER_REDECLARATION, REDECLARATION!>A<!> {
    val x : Int = 0

    val A = 1
  }

  class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {}

  val <!PACKAGE_OR_CLASSIFIER_REDECLARATION, REDECLARATION!>A<!> = 1

// FILE: f.kt
  package redeclarations.<!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
    class A {}
