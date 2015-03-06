package Jet86

class A {
  default <!REDECLARATION!>object<!> {
    val x = 1
  }
  default <!MANY_DEFAULT_OBJECTS, REDECLARATION!>object<!> {
    val x = 1
  }
}

class AA {
  default object {
    val x = 1
  }
  default <!MANY_DEFAULT_OBJECTS!>object A<!> {
    val x = 1
  }
    default <!MANY_DEFAULT_OBJECTS!>object AA<!> {
    val x = 1
  }
}

class B() {
  val x = 12
}

object b {
  default <!DEFAULT_OBJECT_NOT_ALLOWED!>object<!> {
    val x = 1
  } // error
}

val a = A.x
val c = B.<!UNRESOLVED_REFERENCE!>x<!>
val d = b.<!UNRESOLVED_REFERENCE!>x<!>

val s = <!NO_DEFAULT_OBJECT!>System<!>  // error
fun test() {
  System.out.println()
  java.lang.System.out.println()
}