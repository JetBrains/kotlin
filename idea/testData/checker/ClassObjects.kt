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
  <error descr="[COMPANION_OBJECT_NOT_ALLOWED] A companion object is not allowed here">companion</error> object {
    val x = 1
  }
  // error
}

val a = A.x
val c = B.<error>x</error>
val d = b.<error>x</error>

val s = <error>System</error>  // error
fun test() {
  System.out.println()
  java.lang.System.out.println()
}