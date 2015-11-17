// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
package Jet86

class A {
  companion <!REDECLARATION!>object<!> {
    val x = 1
  }
  <!MANY_COMPANION_OBJECTS!>companion<!> <!REDECLARATION!>object<!> {
    val x = 1
  }
}

class AA {
  companion object {
    val x = 1
  }
  <!MANY_COMPANION_OBJECTS!>companion<!> object A {
    val x = 1
  }
    <!MANY_COMPANION_OBJECTS!>companion<!> object AA {
    val x = 1
  }
}

class B() {
  val x = 12
}

object b {
  <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {
    val x = 1
  } // error
}

val a = A.x
val c = B.<!UNRESOLVED_REFERENCE!>x<!>
val d = b.<!UNRESOLVED_REFERENCE!>x<!>

val s = <!NO_COMPANION_OBJECT!>System<!>  // error
fun test() {
  System.out.println()
  java.lang.System.out.println()
}