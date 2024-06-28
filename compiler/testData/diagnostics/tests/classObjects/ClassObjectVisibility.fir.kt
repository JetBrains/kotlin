package foo

fun test() {
  A.d
  A.Companion.<!INVISIBLE_REFERENCE!>f<!>
  B.<!INVISIBLE_REFERENCE!>D<!>
  <!INVISIBLE_REFERENCE!>CCC<!>
  CCC.<!INVISIBLE_REFERENCE!>classObjectVar<!>
  E.F.G
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

class E {
    class F {
        companion object G
    }
    private companion object
}
