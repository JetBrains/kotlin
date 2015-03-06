package Jet86

class A {
  default <!REDECLARATION!>object<!> {
    val x = 1
  }
  <!MANY_DEFAULT_OBJECTS!>default<!> <!REDECLARATION!>object<!> {
    val x = 1
  }
}

class AA {
  default object {
    val x = 1
  }
  <!MANY_DEFAULT_OBJECTS!>default<!> object A {
    val x = 1
  }
    <!MANY_DEFAULT_OBJECTS!>default<!> object AA {
    val x = 1
  }
}

class B() {
  val x = 12
}

object b {
  <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object {
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