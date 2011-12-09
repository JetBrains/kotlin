// JET-81 Assertion fails when processing self-referring anonymous objects

val y = object {
  val a = <error>y</error>
}

val z = y.a;

object A {
  val x = A
}

val a = object {
  {
    b + 1
  }
  val x = b
  val y = 1
}

val b = <error>a</error>.x
val c = a.y
