package foo

fun test() {
  A.d
  A.Companion.<!INVISIBLE_MEMBER!>f<!>
  B.<!INVISIBLE_MEMBER!>D<!>
  <!INVISIBLE_MEMBER!>CCC<!>
  CCC.<!INVISIBLE_MEMBER!>classObjectVar<!>
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
