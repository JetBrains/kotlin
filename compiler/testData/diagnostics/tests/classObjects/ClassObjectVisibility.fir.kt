package foo

fun test() {
  A.d
  A.Companion.<!INVISIBLE_REFERENCE!>f<!>
  B.D
  CCC
  CCC.classObjectVar
}

class A() {
  public companion object {
    val d = 3
    private object f {

    }
  }
}

class B {
    class D {
        private companion object
    }
}

class CCC() {
  private companion object {
    val classObjectVar = 3
  }
}
