// FILE: a/M.java
package a;

public class M {
    public class Inner {

    }

    public static class Nested {

    }

    private class PrInner {

    }

    private static class PrNested {

    }
}

// FILE: b.kt
package b

fun f() {
  val <!UNUSED_VARIABLE!>c1<!>: a.M.Inner
  val <!UNUSED_VARIABLE!>c2<!>: a.M.Nested
  val <!UNUSED_VARIABLE!>c3<!>: a.M.<!INVISIBLE_REFERENCE!>PrInner<!>
  val <!UNUSED_VARIABLE!>c4<!>: a.M.<!INVISIBLE_REFERENCE!>PrNested<!>

}

