// NI_EXPECTED_FILE
// JET-81 Assertion fails when processing self-referring anonymous objects

class Test {
  private val y = object {
    val a = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>y<!>;
  }

  val z = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>y.a<!>;

}

object A {
  val x = A
}

class Test2 {
  private val a = object {
    init {
      <!UNINITIALIZED_VARIABLE!>b<!> + 1
    }
    val x = <!UNINITIALIZED_VARIABLE!>b<!>
    val y = 1
  }

  val b = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>a<!>.<!UNRESOLVED_REFERENCE!>x<!>
  val c = a.y
}
