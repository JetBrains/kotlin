// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
// JET-81 Assertion fails when processing self-referring anonymous objects

class Test {
  private val y = object {
    val a = y;
  }

  val z = y.<!UNRESOLVED_REFERENCE!>a<!>;

}

object A {
  val x = A
}

class Test2 {
  private val a = object {
    init {
      b + 1
    }
    val x = b
    val y = 1
  }

  val b = a.x
  val c = a.y
}