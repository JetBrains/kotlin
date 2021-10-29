// NI_EXPECTED_FILE
// JET-81 Assertion fails when processing self-referring anonymous objects

class Test {
  private val y = object {
    val a = <!DEBUG_INFO_MISSING_UNRESOLVED, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!>y<!>;
  }

  val z = y.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>;

}

object A {
  val x = A
}

class Test2 {
  private val a = object {
    init {
      <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, UNINITIALIZED_VARIABLE!>b<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1
    }
    val x = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, UNINITIALIZED_VARIABLE!>b<!>
    val y = 1
  }

  val b = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>x<!><!>
  val c = a.y
}
