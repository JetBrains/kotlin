package Jet86

class A {
  class object {
    val x = 1
  }
  <error descr="[MANY_DEFAULT_OBJECTS] Only one default object is allowed per class">class object Another</error> { // error
    val x = 1
  }
}

class B() {
  val x = 12
}

object b {
  <error descr="[DEFAULT_OBJECT_NOT_ALLOWED] A default object is not allowed here">class object</error> {
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