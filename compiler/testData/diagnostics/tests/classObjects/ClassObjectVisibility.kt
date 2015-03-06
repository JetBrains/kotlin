package foo

fun test() {
  A.d
  A.Default.<!INVISIBLE_MEMBER!>f<!>
  B.<!INVISIBLE_MEMBER!>D<!>
  <!INVISIBLE_MEMBER!>CCC<!>
  CCC.<!INVISIBLE_MEMBER!>classObjectVar<!>
}

class A() {
  public default object {
    val d = 3
    private object f {

    }
  }
}

class B {
    class D {
        private default object
    }
}

class CCC() {
  private default object {
    val classObjectVar = 3
  }
}