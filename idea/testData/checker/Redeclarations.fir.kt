//package redeclarations {
  <error descr="[REDECLARATION] Conflicting declarations: [A, /A]">object A</error> {
    val x : Int = 0

    val A = 1
  }

  <error descr="[REDECLARATION] Conflicting declarations: [A, /A]">class A {}</error>

  <error descr="[REDECLARATION] Conflicting declarations: [A, A]">val A = 1</error>

//}
