package foo

fun test() {
  A.d
  A.<!INVISIBLE_MEMBER!>f<!>
  <!INVISIBLE_MEMBER!>CCC<!>.<!INVISIBLE_MEMBER!>classObjectVar<!>
}

class A() {
  public class object {
    val d = 3
    private object f {

    }
  }
}

class CCC() {
  private class object {
    val classObjectVar = 3
  }
}