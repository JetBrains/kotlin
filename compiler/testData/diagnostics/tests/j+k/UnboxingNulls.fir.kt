// FILE: a/Test.java
package a;

public class Test<T> {
  T t() {return null;}
}
// FILE: b.kt
package a

fun foo() {
  // If this fails, it means that we have broken the rule that Java returns are always nullable
  a.<!UNRESOLVED_REFERENCE!>Test<!><Int>().<!UNRESOLVED_REFERENCE!>t<!>() + 1
}
