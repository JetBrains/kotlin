package Jet86

class A {
  companion object {
    val x = 1
  }
  <error descr="[MANY_COMPANION_OBJECTS] Only one companion object is allowed per class">companion</error> object Another { // error
    val x = 1
  }
}

class B() {
  val x = 12
}

object b {
  companion object {
    val x = 1
  }
  // error
}

val a = A.x
val c = B.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: x">x</error>
val d = b.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: x">x</error>

val s = System  // error
fun test() {
  System.out.println()
  java.lang.System.out.println()
}
