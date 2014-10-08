package Jet86

class A {
  class object {
    val x = 1
  }
  <error>class object { // error
    val x = 1
  }</error>
}

class B() {
  val x = 12
}

object b {
  <error>class object {
    val x = 1
  }</error>
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