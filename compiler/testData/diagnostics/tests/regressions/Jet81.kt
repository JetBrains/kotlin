// JET-81 Assertion fails when processing self-referring anonymous objects

val y = object {
  val a = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>y<!>;
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

val b = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>a<!>.x
val c = a.y
