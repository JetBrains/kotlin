// FIR_IDENTICAL
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
  val c1: a.M.Inner
  val c2: a.M.Nested
  val c3: a.M.<!INVISIBLE_REFERENCE!>PrInner<!>
  val c4: a.M.<!INVISIBLE_REFERENCE!>PrNested<!>

}
